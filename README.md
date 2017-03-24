# TheAutoMaTweet

This is a scala solution of the challenge found in [this](https://github.com/LambdaSharp/March2017-ImageTweeterChallenge) repository


## Setup

### Create a new bucket
 
```bash
aws s3api create-bucket --bucket <DEPLOYMENT BUCKET NAME>
aws s3api create-bucket --bucket <IMAGES BUCKET NAME>
```


### Create a role for the lambda function
This role will contain the policy allowing the lambda function to get objects from S3 and use the Rekognition API

```bash
aws iam create-role \
    --role-name scala-lambda-role \
    --assume-role-policy-document file://trust-policy.json
    
{
    "Role": {
        "AssumeRolePolicyDocument": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": "sts:AssumeRole",
                    "Effect": "Allow",
                    "Principal": {
                        "Service": "lambda.amazonaws.com"
                    }
                }
            ]
        },
        "RoleId": "FEDCBA987654321",
        "CreateDate": "2017-03-24T04:39:08.518Z",
        "RoleName": "scala-lambda-role",
        "Path": "/",
        "Arn": "arn:aws:iam::123456789012:role/scala-lambda-role"
    }
}
```

#### Create a policy
I have included a simple policy file. **PLEASE NOTE THIS POLICY IS VERY OPEN AND SHOULD ONLY BE USED FOR TEST APPS**

```bash
aws iam create-policy \
    --policy-name scala-lambda-s3-rekognition-policy \
    --policy-document file://policy.json

{
    "Policy": {
        "PolicyName": "scala-lambda-s3-rekognition-policy",
        "CreateDate": "2017-03-24T05:03:31.319Z",
        "AttachmentCount": 0,
        "IsAttachable": true,
        "PolicyId": "ABCDEF123456",
        "DefaultVersionId": "v1",
        "Path": "/",
        "Arn": "arn:aws:iam::123456789012:policy/scala-lambda-s3-rekognition-policy",
        "UpdateDate": "2017-03-24T05:03:31.319Z"
    }
}
```

> **NOTE**:
> Take a note of the ARN returned by this call!

#### Attach the policy to the role

```bash
aws iam attach-role-policy \
    --role-name scala-lambda-role \
    --policy-arn <POLICY ARN>
```

### Compile the project and upload it to S3
```bash
stb compile
sbt assembly
aws s3 cp target/scala-2.12/LambdaScala-assembly-1.0.jar s3://<DEPLOYMENT BUCKET NAME>/LambdaScala-assembly-1.0.jar
```

### Create the lambda function
```bash
aws lambda create-function \
    --function-name scala-lambda-function \
    --runtime java8 \
    --timeout 30 \
    --memory-size 256 \
    --handler "lambda.AutoMaTweet::lambdaHandler" \
    --code S3Bucket=<DEPLOYMENT BUCKET NAME>,S3Key=LambdaScala-assembly-1.0.jar \
    --role <ROLE ARN>
```

### Wrapping it all up

#### Create a trigger
At this point login to AWS go to lambda and click on your new function. 

Select `Triggers` > `Add trigger` > `S3`
 
Make sure to select the **Bucket**: `<IMAGES BUCKET NAME>` and the **Event type**: `Object Created (All)` and click on `Submit`

#### Set the environment variables
These values are used to access your twitter application (go to https://apps.twitter.com/ if you don't have one).
Create the following environment variables in the `Code` section of your lambda function: `CONSUMER_KEY`, `CONSUMER_SECRET`, `ACCESS_TOKEN`, `TOKEN_SECRET` 
and fill their values with your twitter application keys and access tokens. Save! 

## Upload an image
Upload an image `jpg` or `png` to the `<IMAGES BUCKET NAME>`.


## Troubleshooting
If you are not seeing anything happen, got the `Monitoring` section of your lambda function and click on `View logs in CloudWatch`, 
this will open the CloudWatch logs for your lambda function. Select the most resent Log Stream.
