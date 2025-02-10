/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2004 by JSTOR and the President and Fellows of Harvard College
 *
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.jpeg2000;

import edu.harvard.hul.ois.jhove.*;
import edu.harvard.hul.ois.jhove.module.Jpeg2000Module;
import java.io.*;
import java.util.*;

/**
 * Encapsulation of a JPEG 2000 codestream.
 * 
 * This is based on the information in Appendix A of
 * ISO/IEC 15444-1:2000(E).  That standard "does not 
 * include a definition of compliance or conformance."
 * 
 * @author Gary McGath
 *
 */
public class ContCodestream {
    
    private Codestream _codestream;
    private long _length;
    private Jpeg2000Module _module;
    private DataInputStream _dstream;
//    private List _tileParts;
    private List<Tile> _tiles;
    private long _tileLeft;
    
    /* Tile for which we have most recently seen an unclosed SOT */
    private Tile _curTile;
    
    /* Set to true when a PPM marker segment is found */
    private boolean ppmSeen;
    
    /* Constants defining codestream markers */
    private final static int
        SOC = 0X4F,             // start of codestream
        COD = 0X52,             // coding style default
        COC = 0X53,             // coding style component
        TLM = 0X55,             // tile-part lengths
        PLM = 0X57,             // packet length, main header
        PLT = 0X58,             // packet length, tile-part header
        QCD = 0X5C,             // quantization default
        QCC = 0X5D,             // quantization component
        RGN = 0X5E,             // region of interest
        POC = 0X5F,             // progression order change
        PPM = 0X60,             // Packed packet headers, main header
        PPT = 0X61,             // packed packet headers, tile-part header
        CRG = 0X63,             // component registration
        COM = 0X64,             // comment
        SOT = 0X90,             // start of tile part
        SOP = 0X91,             // start of packet
        EPH = 0X92,             // end of packet header
        SOD = 0X93,             // start of data
        EOC = 0XD9,             // end of codestream
        SIZ = 0X51;             // image and tile size
    
    /**
     *  Constructor.
     * 
     *  @param length   Length of the codestream, exclusive of the
     *                  box header.  If the codestream box has a length
     *                  field of 0, pass 0 for this parameter.
     */
    public ContCodestream (Jpeg2000Module module, 
            DataInputStream dstream,
            long length)
    {
        _module = module;
        _dstream = dstream;
        _length = length;
        _tiles = new LinkedList<> ();
        //_tileParts = new LinkedList ();   // Do I want both lists?
        ppmSeen = false;
    }



    /** Reading a codestream generates various bits of information about
     *  the image.  These are made available after reading through
     *  accessor functions.
     * 
     *  @param   cs     The image which this codestream defines.
     *                  Must have a non-null <code>codestream</code> 
     *                  field.
     *  
     *  @param   info   The RepInfo object which accumulates information
     *                  about the document.  Used for reporting errors.
     * 
     *  @return  True if no fatal errors detected, 
     *           false if error prevents safe continuation
     */
    public boolean readCodestream (Codestream cs, RepInfo info) 
                throws IOException
    {
        _codestream = cs;
        long lengthLeft = _length;
        _tileLeft = 0;
        boolean socSeen = false;  // flag to note an SOC marker has been seen
        
        // length may be 0, signifying that we go till EOF
        if (lengthLeft == 0) {
            lengthLeft = Long.MAX_VALUE;
        }
        try {
            while (lengthLeft > 0) {
                // Look for the start of a marker (0xFF).
                int ff = ModuleBase.readUnsignedByte (_dstream, _module);
                if (ff != 0XFF) {
                    info.setMessage (new ErrorMessage(MessageConstants.JPEG2000_HUL_8));
                    info.setWellFormed (false);
                    return false;
                }
                // Now read the type of marker (one byte).
                int marker = ModuleBase.readUnsignedByte (_dstream, _module);
                if (marker == SOC) {
                    // we got the SOC marker, as expected
                    socSeen = true;
                }
                // Some markers are followed by additional data called
                // parameters. The marker together with its (optional)
                // parameters are called a marker segment. We now prepare a
                // parser for the specific type of marker segment we
                // encountered.
                MarkerSegment ms = MarkerSegment.markerSegmentMaker (marker);
                ms.setCodestream (cs);
                ms.setContCodestream (this);
                ms.setDataInputStream (_dstream);
                ms.setRepInfo (info);
                ms.setModule (_module);
                // Read the length of the marker segment. This includes 2 bytes
                // for the length field itself but *not* the length of the
                // 2-byte marker, so a marker segment actually has a size of
                // markLen + 2.
                int markLen = ms.readMarkLen ();
                // Run the parser for this marker segment. The parameter of the
                // process method expects the number of bytes that must be
                // consumed. A marker segment without parameters (just a marker)
                // implicitly has length 0. A marker segment with parameters has
                // length markLen, but 2 bytes were already consumed by the
                // readMarkLen method when reading the length field, so in this
                // case we have to consume the remaining (markLen - 2) bytes.
                if (!ms.process (markLen == 0 ? 0 : markLen - 2)) {
                    info.setMessage (new ErrorMessage 
                        (MessageConstants.JPEG2000_HUL_9));
                        info.setWellFormed (false);
                        return false;
                }
                // An SOT marker marks the beginning of a tile-part. It has yet
                // another length field apart from the length of the marker
                // segment. This length field contains the length of the whole
                // tile-part from the very beginning of this SOT marker segment
                // to the end of data of that tile-part; it comprises several
                // other marker segments. The SOTMarkerSegment parser writes
                // this length to the _tileLeft attribute. Now, if _tileLeft is
                // 0, this tile-part is assumed to contain all data until the
                // end of the current codestream (see section A.4.2 (page 22) in
                // https://web.archive.org/web/20130810200214/http://www.jpeg.org/public/fcd15444-1.pdf).
                // In other words, if _tileLeft is 0, its length should be the
                // remaining length of the enclosing codestream, which is
                // recorded in the lengthLeft variable.
                if (marker == SOT && _tileLeft == 0) {
                    _tileLeft = lengthLeft;
                }

                // We're done parsing this marker segment. Let's update the
                // count of bytes remaining in this codestream (and, if
                // applicable, tile-part) before parsing the next marker segment
                // in the next iteration of the while loop. As explained above,
                // a marker segment has a size of markLen + 2 bytes, where
                // markLen is 0 for marker segments without parameters.
                lengthLeft -= markLen + 2;
                if (_tileLeft > 0) {
                    _tileLeft -= markLen + 2;
                }
                // A SOD marker segment is followed by a bitstream (raw data).
                // We skip the number of bytes not yet deducted from _tileLeft.
                if (marker == SOD) {
                    _module.skipBytes (_dstream, _tileLeft, _module);
                    lengthLeft -= _tileLeft;
                    _tileLeft = 0;
                }
                if (marker == EOC) {
                    // End of codestream, we're done.
                    break;
                }
            }
        }
        catch (EOFException e) {
            // we're done
        }
        if (!socSeen) {
            info.setMessage (new ErrorMessage(MessageConstants.JPEG2000_HUL_8));
            info.setWellFormed (false);
            return false;
        }
        _codestream.setTiles (_tiles);
        return true;
    }
    
    
    /** Returns the list of tiles.  The elements are Tile objects. */
    public List<Tile> getTiles ()
    {
        return _tiles;
    }
    
    /** Set the number of bytes remaining in the current tile.
     *  For use by MarkerSegment subclasses.
     */
    protected void setTileLeft (long tileLeft)
    {
        _tileLeft = tileLeft;
    }



    
    /** Gets the tile whose index is idx. */
    protected Tile getTile (int idx)
    {
        // If we haven't reached this index before, add a tile.
        // Tiles are supposed to be added sequentially, but
        // PIAV.
        while (_tiles.size () <= idx) {
            _tiles.add (new Tile ());
        }
        return _tiles.get (idx);
    }
    
    /** Sets the value of curTile. */
    protected void setCurTile (Tile tile)
    {
        _curTile = tile;
    }
    
    /** Sets the value of the ppmSeen flag, signifying that
     *  a PPM marker segment has been encountered. */
    protected void setPPMSeen (boolean b)
    {
        ppmSeen = b;
    }
    
    
    /** Gets the value of curTile.  May be null. */
    protected Tile getCurTile ()
    {
        return _curTile;
    }
    
    /** Returns the value of the ppmSeen flag, signifying that
     *  a PPM marker segment has been encountered. */
    protected boolean isPPMSeen ()
    {
        return ppmSeen;
    }

    
    /* Based on marker code, return true if this is a marker
     * segment (i.e., it has parameters).  The documentation
     * isn't fully clear, but I think the only way to determine
     * what is a marker is to enumerate all values that are
     * markers. */
    private static boolean isSegment (int marker) 
    {
        // end of codestream
        
        return !((marker >= 0X30 && marker <= 0X3F) ||
                marker == SOC ||       // start of codestream
                marker == EPH ||       // end of packet header
                marker == SOD ||       // start of data
                marker == EOC);
    }
}
