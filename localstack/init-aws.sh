#!/bin/bash
# LocalStack initialization script
# Creates the S3 buckets that form the Data Lake

set -e

echo "==> Initializing LocalStack AWS resources..."

# Raw / landing zone bucket (Data Lake)
awslocal s3 mb s3://sti-data-lake
awslocal s3api put-bucket-versioning \
    --bucket sti-data-lake \
    --versioning-configuration Status=Enabled

# Create folder prefixes (pseudo-directories)
awslocal s3api put-object --bucket sti-data-lake --key raw/access-logs/   --content-length 0 --body /dev/null
awslocal s3api put-object --bucket sti-data-lake --key raw/sales-events/  --content-length 0 --body /dev/null
awslocal s3api put-object --bucket sti-data-lake --key raw/product-events/ --content-length 0 --body /dev/null
awslocal s3api put-object --bucket sti-data-lake --key processed/          --content-length 0 --body /dev/null
awslocal s3api put-object --bucket sti-data-lake --key curated/            --content-length 0 --body /dev/null

echo "==> S3 Data Lake buckets and prefixes created successfully."
awslocal s3 ls
