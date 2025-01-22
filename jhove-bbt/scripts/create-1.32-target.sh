#!/usr/bin/env bash

testRoot="test-root"
paramCandidateVersion=""
paramBaselineVersion=""
baselineRoot="${testRoot}/baselines"
candidateRoot="${testRoot}/candidates"
targetRoot="${testRoot}/targets"
# Check the passed params to avoid disapointment
checkParams () {
	OPTIND=1	# Reset in case getopts previously used

	while getopts "h?b:c:" opt; do	# Grab the options
		case "$opt" in
		h|\?)
			showHelp
			exit 0
			;;
		b)	paramBaselineVersion=$OPTARG
			;;
		c)	paramCandidateVersion=$OPTARG
			;;
		esac
	done

	if [ -z "$paramBaselineVersion" ] || [ -z "$paramCandidateVersion" ]
	then
		showHelp
		exit 0
	fi

	baselineRoot="${baselineRoot}/${paramBaselineVersion}"
	candidateRoot="${candidateRoot}/${paramCandidateVersion}"
	targetRoot="${targetRoot}/${paramCandidateVersion}"
}

# Show usage message
showHelp() {
	echo "usage: create-target [-b <baselineVersion>] [-c <candidateVersion>] [-h|?]"
	echo ""
	echo "  baselineVersion  : The version number id for the baseline data."
	echo "  candidateVersion : The version number id for the candidate data."
	echo ""
	echo "  -h|? : This message."
}

# Execution starts here
checkParams "$@";
if [[ -d "${targetRoot}" ]]; then
	echo " - removing existing baseline at ${targetRoot}."
	rm -rf "${targetRoot}"
fi

echo "TEST BASELINE: Creating baseline"
# Simply copy baseline for now we're not making any changes
echo " - copying ${baselineRoot} baseline to ${targetRoot}"
cp -R "${baselineRoot}" "${targetRoot}"

# Patch release details of the reporting module in the audit file
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/outputHandler release="1.11">XML/outputHandler release="1.13">XML/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/outputHandler release="1.2">JSON/outputHandler release="1.4">JSON/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/outputHandler release="1.6">TEXT/outputHandler release="1.8">TEXT/' {} \;

# Update release details for HTML module
find "${targetRoot}" -type f -name "*.html.jhove.xml" -exec sed -i 's/<reportingModule release="1.4.3" date="2023-03-16">HTML-hul<\/reportingModule>/<reportingModule release="1.4.4" date="2024-08-22">HTML-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.4.3">HTML-hul<\/module>/<module release="1.4.4">HTML-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/<release>1.4.3<\/release>/<release>1.4.4<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/2023-03-16/2024-08-22/' {} \;

# Update release details for PDF module
find "${targetRoot}" -type f -name "*.pdf.jhove.xml" -exec sed -i 's/<reportingModule release="1.12.6" date="2024-07-31">PDF-hul<\/reportingModule>/<reportingModule release="1.12.7" date="2024-08-22">PDF-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.12.6">PDF-hul<\/module>/<module release="1.12.7">PDF-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-PDF-hul.jhove.xml" -exec sed -i 's/<release>1.12.6<\/release>/<release>1.12.7<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-PDF-hul.jhove.xml" -exec sed -i 's/2024-07-31/2024-08-22/' {} \;

# Update release details for TIFF module
find "${targetRoot}" -type f -name "*.tiff.jhove.xml" -exec sed -i 's/<reportingModule release="1.9.4" date="2023-03-16">TIFF-hul<\/reportingModule>/<reportingModule release="1.9.5" date="2024-08-22">TIFF-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "*.tif.jhove.xml" -exec sed -i 's/<reportingModule release="1.9.4" date="2023-03-16">TIFF-hul<\/reportingModule>/<reportingModule release="1.9.5" date="2024-08-22">TIFF-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "*.g3.jhove.xml" -exec sed -i 's/<reportingModule release="1.9.4" date="2023-03-16">TIFF-hul<\/reportingModule>/<reportingModule release="1.9.5" date="2024-08-22">TIFF-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.9.4">TIFF-hul<\/module>/<module release="1.9.5">TIFF-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-TIFF-hul.jhove.xml" -exec sed -i 's/<release>1.9.4<\/release>/<release>1.9.5<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-TIFF-hul.jhove.xml" -exec sed -i 's/2023-03-16/2024-08-22/' {} \;

# Update release details for XML module
find "${targetRoot}" -type f -name "*.xml.jhove.xml" -exec sed -i 's/<reportingModule release="1.5.4" date="2024-03-05">XML-hul<\/reportingModule>/<reportingModule release="1.5.5" date="2024-08-22">XML-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.5.4">XML-hul<\/module>/<module release="1.5.5">XML-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-XML-hul.jhove.xml" -exec sed -i 's/<release>1.5.4<\/release>/<release>1.5.5<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-XML-hul.jhove.xml" -exec sed -i 's/2024-03-05/2024-08-22/' {} \;

# Copy the TIFF Module results changed by https://github.com/openpreserve/jhove/pull/915
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/AA_Banner.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/AA_Banner.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/AA_Banner.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/strike.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/strike.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/strike.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/testpage-large.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/testpage-large.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/testpage-large.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/testpage-medium.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/testpage-medium.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/testpage-medium.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/oxford.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/oxford.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/oxford.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/jim___gg.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/jim___gg.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/jim___gg.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/bathy1.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/bathy1.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/bathy1.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/jim___cg.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/jim___cg.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/jim___cg.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/quad-tile.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/quad-tile.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/quad-tile.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/compos.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/compos.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/compos.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/pagemaker.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/pagemaker.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/pagemaker.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/jello.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/jello.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/jello.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/little-endian.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/little-endian.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/little-endian.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/cramps-tile.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/cramps-tile.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/cramps-tile.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/jim___ah.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/jim___ah.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/jim___ah.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/g3test.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/g3test.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/g3test.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/6mp_soft.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/6mp_soft.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/6mp_soft.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/ycbcr-cat.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/ycbcr-cat.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/ycbcr-cat.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/quad-lzw.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/quad-lzw.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/quad-lzw.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/jim___dg.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/jim___dg.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/jim___dg.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/fax2d.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/fax2d.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/fax2d.tif.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/TIFF-hul/peppers.tif.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/TIFF-hul/peppers.tif.jhove.xml" "${targetRoot}/examples/modules/TIFF-hul/peppers.tif.jhove.xml"
fi

# Copy the TIFF fix affected files from the candidate to the target
declare -a tiff_affected=("examples/modules/TIFF-hul/cramps.tif.jhove.xml"
				"examples/modules/TIFF-hul/text.tif.jhove.xml"
				"examples/modules/TIFF-hul/testpage-small.tif.jhove.xml")
for filename in "${tiff_affected[@]}"
do
	if [[ -f "${candidateRoot}/${filename}" ]]; then
		cp "${candidateRoot}/${filename}" "${targetRoot}/${filename}"
	fi
done

# Copy the XHTML fix affected files from the candidate to the target
declare -a xhtml_affected=("errors/modules/HTML-hul/xhtml-trans-no-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-strict-no-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-frames-no-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-1-1-no-xml-dec.html.jhove.xml")
for filename in "${xhtml_affected[@]}"
do
	if [[ -f "${candidateRoot}/${filename}" ]]; then
		cp "${candidateRoot}/${filename}" "${targetRoot}/${filename}"
	fi
done

# Copy the XML fix affected files from the candidate to the target
declare -a xhtml_affected=("errors/modules/HTML-hul/xhtml-trans-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-strict-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-frames-xml-dec.html.jhove.xml"
				"errors/modules/HTML-hul/xhtml-1-1-xml-dec.html.jhove.xml"
				"examples/modules/XML-hul/valid-external.dtd.jhove.xml"
				"examples/modules/XML-hul/external-unparsed-entity.ent.jhove.xml"
				"examples/modules/XML-hul/external-parsed-entity.ent.jhove.xml")
for filename in "${xhtml_affected[@]}"
do
	if [[ -f "${candidateRoot}/${filename}" ]]; then
		cp "${candidateRoot}/${filename}" "${targetRoot}/${filename}"
	fi
done

# Copy all of the AIF and WAV results as these are changed by the AES schema changes
cp -rf "${candidateRoot}/examples/modules/AIFF-hul" "${targetRoot}/examples/modules/"
cp -rf "${candidateRoot}/examples/modules/WAVE-hul" "${targetRoot}/examples/modules/"
cp -rf "${candidateRoot}/errors/modules/WAVE-hul" "${targetRoot}/errors/modules/"

# Copy the results of the new XML fixes for multiple redirect lookups and to ensure no regression for repeat XML warnings
cp -rf "${candidateRoot}/errors/modules/XML-hul" "${targetRoot}/errors/modules/"

# Copy the results of the PDF offset message fix
declare -a pdf_offset_affected=("errors/modules/PDF-hul/pdf-hul-5-govdocs-659152.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-10-govdocs-803945.pdf.jhove.xml"
				"regression/modules/PDF-hul/issue_306.pdf.jhove.xml")
for filename in "${pdf_offset_affected[@]}"
do
	if [[ -f "${candidateRoot}/${filename}" ]]; then
		cp "${candidateRoot}/${filename}" "${targetRoot}/${filename}"
	fi
done

declare -a wiki_link_affected=("errors/modules/PDF-hul/pdf-hul-73-bug-size-int.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-52-govdocs-983827.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-79-govdocs-095305.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-43-govdocs-486355.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-34-govdocs-259511.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-82-govdocs-333472.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-51-govdocs-085551.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-76-372051162.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-61-CERN-2005-009.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-29-govdocs-375118.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-13-govdocs-346874.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-9-govdocs-065694.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-1-govdocs-519846.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-86-govdocs-445892.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-62-567147525.pdf.jhove.xml"
				"errors/modules/PDF-hul/corruptionOneByteMissing.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-76-govdocs-289573.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-35-govdocs-156429.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-5-govdocs-659152.pdf.jhove.xml"
				"errors/modules/PDF-hul/pdf-hul-49-32932439X.pdf.jhove.xml"
				"errors/modules/JPEG2000-hul/signature_corrupted.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/隨機中國文字.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1984.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/missing_null_terminator_in_urlbox.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/ランダム日本語テキスト.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/invalid_character_in_xml.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/is_codestream.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/truncated_at_byte_5000.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1971.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/openJPEG15.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/meth_is_2_no_icc.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1999.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/is_jpeg.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1920.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/triggerUnboundLocalError.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/README.md.jhove.xml"
				"errors/modules/JPEG2000-hul/last_byte_missing.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1951.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/is_jpm.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1961.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-2011.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/empty.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/height_image_header_damaged.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/palettedImage.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1937.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/data_missing_in_last_tilepart.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-1911.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/bitwiser-icc-corrupted-tagcount-2021.jp2.jhove.xml"
				"errors/modules/JPEG2000-hul/reference.jp2.jhove.xml"
				"regression/modules/JPEG-hul/19_e190014.jpg.jhove.xml"
				"regression/modules/PDF-hul/issue_662.pdf.jhove.xml"
				"regression/modules/PDF-hul/pdf-hul-55-govdocs-616137.pdf.jhove.xml"
				"regression/modules/PDF-hul/class-cast.pdf.jhove.xml"
				"regression/modules/PDF-hul/null-string.pdf.jhove.xml"
				"regression/modules/PDF-hul/issue_473_a.pdf.jhove.xml"
				"regression/modules/PDF-hul/extensions-adbe-other.pdf.jhove.xml"
				"regression/modules/PDF-hul/pdf-hul-59-govdocs-681811.pdf.jhove.xml"
				"regression/modules/PDF-hul/issue_645.pdf.jhove.xml"
				"regression/modules/PDF-hul/null-string-sig-2.pdf.jhove.xml"
				"regression/modules/PDF-hul/issue_531.pdf.jhove.xml"
				"regression/modules/PDF-hul/null-string-sig-1.pdf.jhove.xml"
				"regression/modules/PDF-hul/pdf-hul-65-847453723.pdf.jhove.xml"
				"regression/modules/PDF-hul/pdf-hul-40-govdocs-088919.pdf.jhove.xml"
				"examples/modules/PDF-hul/AA_Banner.pdf.jhove.xml"
				"examples/modules/TIFF-hul/fax2d.g3.jhove.xml"
				"examples/modules/TIFF-hul/chase-tif-f.tif.jhove.xml"
				"examples/modules/TIFF-hul/g3test.g3.jhove.xml"
				"examples/modules/TIFF-hul/smallliz.tif.jhove.xml"
				"examples/modules/TIFF-hul/zackthecat.tif.jhove.xml"
				"examples/modules/HTML-hul/Validate3.2.html.jhove.xml"
				"examples/modules/HTML-hul/Validate4.0Trans.html.jhove.xml"
				"examples/modules/HTML-hul/Validate4.0Frameset.html.jhove.xml"
				"examples/modules/HTML-hul/Validate4.01Trans.html.jhove.xml"
				"examples/modules/HTML-hul/Validate4.01Frameset.html.jhove.xml")
for filename in "${wiki_link_affected[@]}"
do
	if [[ -f "${candidateRoot}/${filename}" ]]; then
		cp "${candidateRoot}/${filename}" "${targetRoot}/${filename}"
	fi
done
