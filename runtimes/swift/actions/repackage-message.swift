func main(params: [String:Any]) -> [String:Any] {
  if let head_commit = params["head_commit"] as? [String:Any] , let repository = params["repository"] as? [String:Any]{
  	if let id = head_commit["id"] , let fullName = repository["full_name"]{
  		print(id)
          print(fullName)
          let message = "commit \(id) was pushed to \(fullName)"
          return ["text": message]
  	}
  }
  return ["text":"Error occurred when attempting to parse information regarding latest git push event."]
}
