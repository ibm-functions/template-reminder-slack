/*
  Takes information from a github event and puts it in a form consumable by
  the whisk.system/slack/post action.
*/
function main(params) {
  const message = 'Your scrum is starting now.  Time to find your team!';
  return { text: message };
}

exports.main = main;
