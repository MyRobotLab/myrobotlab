#!/bin/bash

# Check if version and token arguments are provided
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <version> <github_token>"
  exit 1
fi

# Get the version and GitHub token from the command line arguments
VERSION=$1
TOKEN=$2

# Define the file name based on the version
FILE="target/myrobotlab-${VERSION}.zip"

# Check if the file exists
if [ ! -f "$FILE" ]; then
  echo "File $FILE not found!"
  exit 1
fi

# Create latestVersion.txt and write the version into it
echo "$VERSION" > target/latestVersion.txt

# Check if latestVersion.txt was created successfully
if [ ! -f "target/latestVersion.txt" ]; then
  echo "Failed to create target/latestVersion.txt!"
  exit 1
fi

# Upload the zip file to S3 using the provided version
aws s3 cp "$FILE" s3://myrobotlab-repo/ --acl public-read --profile supertick
# For permalink
aws s3 cp "$FILE" s3://myrobotlab-repo/myrobotlab.zip --acl public-read --profile supertick

# Upload latestVersion.txt to S3
aws s3 cp target/latestVersion.txt s3://myrobotlab-repo/ --acl public-read --profile supertick

# Check if the upload was successful
if [ $? -eq 0 ]; then
  echo "Upload of $FILE to S3 was successful."
else
  echo "Upload failed!"
  exit 1
fi

# Create the JSON payload in a separate file
cat <<EOF > release.json
{
  "tag_name": "$VERSION",
  "target_commitish": "develop",
  "name": "$VERSION Nixie",
  "body": "## MyRobotLab Nixie Release\n\n\
Open Source Framework for Robotics and Creative Machine Control\n\n\
*You know, for robots!*\n\n\
* Project Website: http://myrobotlab.org\n\
* Project Discord: https://discord.gg/AfScp5x8r5\n\
* Download Built Application: [Nixie $VERSION](https://myrobotlab-repo.s3.us-east-1.amazonaws.com/myrobotlab-$VERSION.zip)\n\
* [Javadocs](https://myrobotlab-repo.s3.us-east-1.amazonaws.com/target/site/apidocs/org/myrobotlab/service/package-summary.html)\n\n\
## Base Requirements\n\n\
You will need Java 11 or newer. If you are only running MyRobotLab, you need the JRE (Java Runtime Environment). If you are going to be building from source, you'll need the JDK (Java Development Kit). Oracle or OpenJDK will work.",
  "draft": false,
  "prerelease": false,
  "generate_release_notes": true
}
EOF


# Post the release to GitHub
curl -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: token $TOKEN" \
  https://api.github.com/repos/MyRobotLab/myrobotlab/releases \
  -d @release.json

# Check if the GitHub release was successful
if [ $? -eq 0 ]; then
  echo "GitHub release created successfully."
else
  echo "Failed to create GitHub release!"
  exit 1
fi
