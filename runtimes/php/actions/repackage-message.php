<?php
//  Takes information from a github event and puts it in a form consumable by
//  the whisk.system/slack/post action.
function main(array $args) : array
{
    $message = "commit " + $args["head_commit"]["id"] + " was pushed to " + $args["repository"]["full_name"];
    return ["text" => $message];
}
?>
