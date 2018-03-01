/*
  Takes information from a github event and puts it in a form consumable by
  the whisk.system/slack/post action.
*/
function main(params){
  var message = "Your scrum is starting now.  Time to find your team!";
  return {text: message}
}
