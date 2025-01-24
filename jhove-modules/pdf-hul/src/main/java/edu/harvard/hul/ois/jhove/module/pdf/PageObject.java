/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2003 by JSTOR and the President and Fellows of Harvard College
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.pdf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import edu.harvard.hul.ois.jhove.messages.JhoveMessage;
import edu.harvard.hul.ois.jhove.module.PdfModule;

/**
 *  Class encapsulating a PDF page object node.
 */
public class PageObject extends DocNode 
{
    private List<PdfStream> _contentStreams = new ArrayList<>();  // contents of the page, defaults to empty

    /**
     *  Superclass constructor.
     *  @param module     The module under which we're operating
     *  @param parent     The parent node in the document tree;
     *                    may be null only for the root node
     *  @param dict       The dictionary object on which this node
     *                    is based
     */
    public PageObject (PdfModule module,
                PageTreeNode parent, 
                PdfDictionary dict) throws PdfMalformedException
    {
        super (module, parent, dict);
        _pageObjectFlag = true;
    }

    /**
     *  Find the content stream(s) for this page.  This is
     *  called when the page tree content stream is built
     *  by PageTreeNode.  <code>getContentStreams</code> may 
     *  subsequently be called to get the content.
     */
    public void loadContent (PdfModule module) throws PdfException
    {
        PdfObject contents = _dict.get("Contents");
        // Contents object can be null, the page is empty.
        if (contents == null) {
            return;
        }
        try {
            contents = module.resolveIndirectObject (contents);
            processContents(module, contents);
        }
        catch (IOException e) {
            throw new PdfMalformedException (MessageConstants.PDF_HUL_26, 0); // PDF-HUL-26
        }
    }

    /**
     *   Returns the List of content streams.  The list elements are
     *   of type PdfStream.
     */
    public List<PdfStream> getContentStreams ()
    {
        return _contentStreams;
    }
    
    /**
     *  Return the page's Annots array of dictionaries, or null if none
     */
    public PdfArray getAnnotations () throws PdfException
    {
        try {
            return (PdfArray) _module.resolveIndirectObject (_dict.get ("Annots"));
        }
        catch (ClassCastException e) {
            throw new PdfInvalidException (MessageConstants.PDF_HUL_21); // PDF-HUL-21
        }
        catch (IOException e) {
            throw new PdfMalformedException (MessageConstants.PDF_HUL_22); // PDF-HUL-22
        }
    }


    /**
     *  Call this function when recursively walking through a document
     *  tree.  This allows nextPageObject () to be return this object
     *  exactly once.
     */
    @Override
    public void startWalk ()
    {
        _walkFinished = false;
    }
    
    /**
     *  Returns this object the first time it is called after startWalk
     *  is called, then null when called again.  This allows a recursive
     *  walk through a document tree to work properly.
     */
    @Override
    public PageObject nextPageObject ()
    {
        if (_walkFinished)
            return null;
        _walkFinished = true;
        return this;
    }

    /**
     *  Called to walk through all page tree nodes and page objects.
     *  Functionally identical with nextPageObject.
     */
    @Override
    public DocNode nextDocNode ()
    {
        return nextPageObject ();
    }
    
    /**
     *  Returns the ArtBox for the page, or null if none.  Throws a
     *  PDFException if there is an ArtBox but it is not a rectangle.
     */
    public PdfArray getArtBox () throws PdfException
    {
        return retrieveAndCheckRectangle(this._dict, "ArtBox",
                MessageConstants.PDF_HUL_23); // PDF-HUL-23
    }

    /**
     *  Returns the TrimBox for the page, or null if none.  Throws a
     *  PDFException if there is an TrimBox but it is not a rectangle.
     */
    public PdfArray getTrimBox () throws PdfException
    {
        return retrieveAndCheckRectangle(this._dict, "TrimBox",
                MessageConstants.PDF_HUL_24); // PDF-HUL-24
    }

    /**
     *  Returns the BleedBox for the page, or null if none.  Throws a
     *  PDFException if there is an BleedBox but it is not a rectangle.
     */
    public PdfArray getBleedBox () throws PdfException
    {
        return retrieveAndCheckRectangle(this._dict, "BleedBox",
                MessageConstants.PDF_HUL_25); // PDF-HUL-25
    }

    public void checkTextStreams(RandomAccessFile raf) throws IOException {
        if (_contentStreams == null) {
            return;
        }
        for (PdfStream pdfStream : _contentStreams) {
            Stream stream = pdfStream.getStream();
            byte[] data = new byte[(int) stream.getLength()];
            stream.initRead(raf);
            stream.read(data);
        }
    }

    private static PdfArray retrieveAndCheckRectangle(final PdfDictionary dict,
            final String dictKey, final JhoveMessage invalidMessage) throws PdfInvalidException {
        PdfArray mbox = null;
        try {
        // Retrieve the object from the passed dictionary
            mbox = (PdfArray) dict.get (dictKey);
        } catch (ClassCastException e) {
            throw new PdfInvalidException(invalidMessage);
        }
        if (mbox == null) {
            // If it's null it doesn't exist so return null
            return null;
        }
        else if (mbox.toRectangle () != null) {
            // If the returned array is a rectangle the return it
            return mbox;
        }
        else {
            // The retrieved object isn't a rectangle throw the exception
            throw new PdfInvalidException (invalidMessage);
        }
    }

    private void processContents(PdfModule module, final PdfObject contents) throws PdfException, IOException {
        // The Contents entry in the dictionary may be either
        // a stream or an array of streams.
        if (contents instanceof PdfStream) {
            _contentStreams = new ArrayList<>(1);
            _contentStreams.add((PdfStream) contents);
            return;
        }
        else if (contents instanceof PdfArray) {
            loadContentFromArray(module, (PdfArray) contents);
        }
        else {
            throw new PdfInvalidException (MessageConstants.PDF_HUL_27, 0); // PDF-HUL-27
        }
    }

    private void loadContentFromArray(PdfModule module, final PdfArray contents) throws PdfException, IOException {
        Vector<PdfObject> contentVec =
            contents.getContent ();
        if (contentVec.size () == 0) {
            return;
        }
        _contentStreams = new ArrayList<>(contentVec.size ());
        for (int i = 0; i < contentVec.size (); i++) {
            PdfObject streamElement = contentVec.elementAt (i);
            streamElement = module.resolveIndirectObject(streamElement);
            if (streamElement instanceof PdfStream) {
                _contentStreams.add ((PdfStream) streamElement);
            }
            else {
                throw new PdfInvalidException (MessageConstants.PDF_HUL_28, 0); // PDF-HUL-28
            }
        }
    }
}
