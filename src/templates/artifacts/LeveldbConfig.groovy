database {
    options {
		createIfMissing = true
	}
}
environments {
    development {
        database {
            path   = '@griffon.project.name@-dev'
            delete = true
        }
    }
    test {
        database {
            path   = '@griffon.project.name@-test'
            delete = true
        }
    }
    production {
        database {
            path = '@griffon.project.name@-prod'
        }
    }
}
