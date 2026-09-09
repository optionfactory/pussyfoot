
build:
	mvn clean package
bump:
	mvn versions:set -DgenerateBackupPoms=false
deploy-central:
	mvn clean deploy -Pcentral
