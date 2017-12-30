## Blueprint that does the equivalent of the following:

  *  `wsk -i package bind /whisk.system/github openwhisk-github --param username <username> --param repository <repositoryName> --param accessToken <githubAccessToken>`  
  * `wsk -i trigger create github-trigger --feed openwhisk-github/webhook --param events push`  
  * `wsk -i action create message-ify-github-response`  
  * `wsk -i rule create connect-github-trigger-to-slack-action github-trigger message-ify-github-response`  
  * `wsk package bind /whisk.system/slack openwhisk-slack --param channel "#<channelName>" --param url "<slackApiWebhook>"  --param username "<username>"`
  * `wsk action create github_slack_integration_sequence --sequence message-ify-github-response,openwhisk-slack/post`
  * `wsk -i rule update connect-github-trigger-to-slack-action github-trigger github_slack_integration_sequence`

## Running the Trigger / Actions
  * `git clone <githubRepositoryCloneURI>`
  * `cd <repositoryName>`
  * `touch test.js`
  * `git add test.js`
  * `git commit -m "testing"`
  * `git push origin master`

## Testing the Trigger / Actions
  * Either using, CURL, postman, or another software of your choice do the following:
  * Set the method to POST request and set the URL to be your wskdeploy endpoint
  * Set `Content-Type` to `application/json` and if your endpoint requires authentication put it in the `Authorization` header.
  * Set the `body` to:
  ```
  {  
  	"gitUrl":"https://github.com/ibm-functions/blueprint-github-trigger-slack",  
  	"manifestPath": "runtimes/node",  
  	"wskApiHost": "<openwhisk api host>",  
  	"wskAuth": "<user:pass for openwhisk host^>",  
  	"envData": {"PACKAGE_NAME": "Push Notification","SLACK_USERNAME": "<slackUsername>","SLACK_URL": "https://hooks.slack.com/services/<whateverTheEndOfYourSlackWebhookURIis>","SLACK_CHANNEL": "#<channelName>","GITHUB_USERNAME":"<githubUsername>","GITHUB_REPOSITORY": "<githubRepoName>","GITHUB_ACCESS_TOKEN": "<githubAccessToken>"}  
  }  
  ```
  * ~ Note ~ : manifestPath can be set to either `runtimes/node`, `runtimes/swift`, `runtimes/php`, `runtimes/python` in order to change the runtime used for the message-ify-github-response source.
  * Make the request
  * Go to the cloned github repo referenced by envData and run: `touch test.js`, `git add test.js`, `git commit -m "trigger slack message"`, `git push origin master`

## Troubleshooting  
  * Go to the git repository that you bound the openwhisk-github package to.
  * Click on settings
  * Click on webhooks
  * See if the event is being sent to the correct location
