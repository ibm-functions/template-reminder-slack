# template-reminder-slack

### Overview
You can use this template to deploy some IBM Cloud Functions assets for you.  The assets created by this template are described in the manifest.yaml file, which can be found at `template-reminder-slack/runtimes/your_language_choice/manifest.yaml`

The assets described by this template are a trigger to fire events on an alarm trigger, an action to choose the message to send to slack, an action from the openwhisk slack package to post to slack, a sequence to tie the two actions together, and a rule that maps the trigger to the sequence.

When this template is deployed and provided with a slack webhook url, it will print out changes made to the cloudant database.  The action assumes that there is a database of cats, each with a name and a color.

You can use the wskdeploy tool to deploy this asset yourself using the manifest and available code.

### Available Languages
This template is available in node.js, php, python & swift.
