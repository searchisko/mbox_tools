The main function of this code is to use `mbox_parser` to:

1. parse mbox files
2. add some *metadata* of top of it
3. get JSON document
4. send it via HTTP to search server for indexing

The *metadata* that is added is relevant to specific DCP configuration. Namely it is:

- project code `sys_project`
- provide `sys_url_view` value
- decide which field will be used for the `sys_content`
- provide `sys_content_content-type` value for selected `sys_content`
- add mailing list type (`announce`, `dev`, `users`, ...)