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

# Update release details for HTML module
find "${targetRoot}" -type f -name "*.html.jhove.xml" -exec sed -i 's/<reportingModule release="1.4.4" date="2024-08-22">HTML-hul<\/reportingModule>/<reportingModule release="1.4.5" date="2024-11-27">HTML-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.4.4">HTML-hul<\/module>/<module release="1.4.5">HTML-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/<release>1.4.4<\/release>/<release>1.4.5<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/2024-08-22/2024-11-27/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/01-08-2002/2002-08-01/' {} \;
find "${targetRoot}" -type f -name "audit-HTML-hul.jhove.xml" -exec sed -i 's/31-05-2001/2001-05-31/' {} \;

# Update release details for JPEG 2000 module
find "${targetRoot}" -type f -name "*.jp2.jhove.xml" -exec sed -i 's/<reportingModule release="1.4.4" date="2023-03-16">JPEG2000-hul<\/reportingModule>/<reportingModule release="1.4.5" date="2024-11-27">JPEG2000-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "*.jpx.jhove.xml" -exec sed -i 's/<reportingModule release="1.4.4" date="2023-03-16">JPEG2000-hul<\/reportingModule>/<reportingModule release="1.4.5" date="2024-11-27">JPEG2000-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "*.md.jhove.xml" -exec sed -i 's/<reportingModule release="1.4.4" date="2023-03-16">JPEG2000-hul<\/reportingModule>/<reportingModule release="1.4.5" date="2024-11-27">JPEG2000-hul<\/reportingModule>/' {} \;
find "${targetRoot}" -type f -name "audit.jhove.xml" -exec sed -i 's/<module release="1.4.4">JPEG2000-hul<\/module>/<module release="1.4.5">JPEG2000-hul<\/module>/' {} \;
find "${targetRoot}" -type f -name "audit-JPEG2000-hul.jhove.xml" -exec sed -i 's/<release>1.4.4<\/release>/<release>1.4.5<\/release>/' {} \;
find "${targetRoot}" -type f -name "audit-JPEG2000-hul.jhove.xml" -exec sed -i 's/2023-03-16/2024-11-27/' {} \;

# Copy the files affected by the relative URL output changes to the XML reporting module
if [[ -f "${candidateRoot}/errors/modules/JPEG2000-hul/ランダム日本語テキスト.jp2.jhove.xml" ]]; then
	cp "${candidateRoot}/errors/modules/JPEG2000-hul/ランダム日本語テキスト.jp2.jhove.xml" "${targetRoot}/errors/modules/JPEG2000-hul/ランダム日本語テキスト.jp2.jhove.xml"
fi
if [[ -f "${candidateRoot}/errors/modules/JPEG2000-hul/隨機中國文字.jp2.jhove.xml" ]]; then
	cp "${candidateRoot}/errors/modules/JPEG2000-hul/隨機中國文字.jp2.jhove.xml" "${targetRoot}/errors/modules/JPEG2000-hul/隨機中國文字.jp2.jhove.xml"
fi

# Copy the files affected by the change to the JPEG-2000 module that prevents empty CompositeListHeader lists from been created
if [[ -f "${candidateRoot}/errors/modules/JPEG2000-hul/is_jpx.jp2.jhove.xml" ]]; then
	cp "${candidateRoot}/errors/modules/JPEG2000-hul/is_jpx.jp2.jhove.xml" "${targetRoot}/errors/modules/JPEG2000-hul/is_jpx.jp2.jhove.xml"
fi
if [[ -f "${candidateRoot}/examples/modules/JPEG2000-hul/ROITest.jpx.jhove.xml" ]]; then
	cp "${candidateRoot}/examples/modules/JPEG2000-hul/ROITest.jpx.jhove.xml" "${targetRoot}/examples/modules/JPEG2000-hul/ROITest.jpx.jhove.xml"
fi
