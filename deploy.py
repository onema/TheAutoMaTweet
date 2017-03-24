import argparse
import logging
from subprocess import call
import boto3

logging.basicConfig()
logging.Logger('deployment')
logger = logging.getLogger('deployment')
logger.setLevel(logging.DEBUG)


def get_args():
    # Input arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('--bucket-name',   '-b', required=True, help='')
    parser.add_argument('--file-location', '-f', default='target/scala-2.12/LambdaScala-assembly-1.0.jar',  help='')
    parser.add_argument('--function-name', '-n', required=True, help='')
    parser.add_argument('--profile', default=None, help='The AWS profile to use')
    return parser.parse_args()


if __name__ == '__main__':
    logger.debug('Parsing arguments')
    args = get_args()

    logger.info('Compiling project')
    call(['sbt', 'compile'])
    call(['sbt', 'assembly'])

    session = boto3.session.Session(profile_name=args.profile)
    s3_client = session.client('s3')

    filename = 'LambdaScala-assembly-1.0.jar'
    logger.info('Uploading file "{0}" to bucket "{1}"'.format(filename, args.bucket_name))
    s3_client.upload_file(args.file_location, args.bucket_name, filename)

    logger.info('Updating lambda code')
    lambda_client = session.client('lambda')
    lambda_client.update_function_code(
        FunctionName=args.function_name,
        S3Bucket=args.bucket_name,
        S3Key='LambdaScala-assembly-1.0.jar'
    )




