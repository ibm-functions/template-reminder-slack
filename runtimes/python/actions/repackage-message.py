def main(params):
    message = "commit " + params.get("head_commit").get("id") + " was pushed to " + params.get("repository").get("full_name");
    return {"text": message}
