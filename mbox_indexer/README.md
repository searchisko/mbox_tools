The main function of this code is to use `mbox_parser` to:

1. parse mbox files
2. add some *metadata* of top of it
3. get JSON document
4. send it via HTTP to search server for indexing

The *metadata* that is added is relevant to specific DCP configuration. Namely it is:

- project code `project` for project name (e.g. `cdi`, `mod_cluster`, `jboss-l10n`)
- add `mail_list_category` (`announce`, `dev`, `users`)
- provide `sys_url_view` value
- decide which field will be used for the `sys_content`
- provide `sys_content_content-type` value for selected `sys_content`
- populate `sys_description` with `message_snippet`
