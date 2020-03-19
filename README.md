# Keycloak Crowd User Storage Library

[![GitHub](https://img.shields.io/github/license/hdensity/keycloak-crowd-user-federation)](https://github.com/hdensity/keycloak-crowd-user-federation/blob/master/LICENSE)
[![Travis (.org)](https://img.shields.io/travis/github/hdensity/keycloak-crowd-user-federation)](https://travis-ci.com/github/hdensity/keycloak-crowd-user-federation)
[![Coverage Status](https://coveralls.io/repos/github/hdensity/keycloak-crowd-user-federation/badge.svg?branch=master)](https://coveralls.io/github/hdensity/keycloak-crowd-user-federation?branch=master)
[![Code Climate maintainability](https://img.shields.io/codeclimate/maintainability/hdensity/keycloak-crowd-user-federation)](https://codeclimate.com/github/hdensity/keycloak-crowd-user-federation)
[![Code Climate issues](https://img.shields.io/codeclimate/issues/hdensity/keycloak-crowd-user-federation)](https://codeclimate.com/github/hdensity/keycloak-crowd-user-federation/issues)
[![Active](http://img.shields.io/badge/Status-Active-green.svg)](https://github.com/hdensity/keycloak-crowd-user-federation) 

This library provides a [Keycloak](https://github.com/keycloak/keycloak) [user federation](https://www.keycloak.org/docs/latest/server_development/#_user-storage-spi) implementation for [Atlassian Crowd](https://www.atlassian.com/software/crowd), providing access to user's, their details and attributes, as well as crowd group memberships.

**Note:** The library provides read only access to the connected Crowd instance. For further information on future developments, please see below.

## Supported environment

The library has been developed using the latest available versions of its Keycloak and Crowd dependencies, and has been tested to run against:

* Keycloak v9
* Crowd Server v4

Other versions have *NOT* been tested, but Keycloak v8 and up, and Crowd Server v3.7 and up should be supported.

## Getting started

### Docker

If you have deployed Keycloak using the official [docker image](https://hub.docker.com/r/jboss/keycloak/), you have the option to:

* **Mount the library:** This approach supports hot redeployment, all you need to do is replace the jar on the host, and Keycloak will autodetect the change and redeploy the library for you.

```
docker run -d --name keycloak \
    --mount type=bind,source=target/crowd-user-storage.jar,target=/opt/jboss/keycloak/standalone/deployments/crowd.jar \
    jboss/keycloak
```

* **Create image:** Create a new image and copy the file to the following location: `/opt/jboss/keycloak/standalone/deployments/crowd.jar`

### Deploy the library

Deploying the library follows the standard Keycloak [approach](https://www.keycloak.org/docs/latest/server_development/#packaging-and-deployment): copy the file to `standalone/deployments/` of your keycloak installation, or use the JBoss CLI to do the deployment for you.

### Create application in Crowd

You need to have an application configured in Crowd, in order for Keycloak to have access to it. You can follow the official documentation [here](https://confluence.atlassian.com/crowd/adding-an-application-18579591.html).

### Enable the Provider for a Realm

To add the provider to your Keycloak realm(s), follow the official documentation [here](https://www.keycloak.org/docs/latest/server_admin/#adding-a-provider). After selecting the `crowd` provider from the list, the following configuration options are available:

#### Required Settings
* `Enabled`: whether to enable the provider
* `Console Display Name`: display name of provider when linked in admin console
* `Priority`: priority of provider when doing a user lookup (lowest first)
* `Crowd URL`: the url to your crowd instance, e.g. http://host.docker.internal:8095/crowd
* `Crowd Application Name`: the name of the application as configured in your crowd instance
* `Crowd Application Password`: the password of the application as configured in your crowd instance.

#### Cache Settings
* `Cache Policy`: the cache policy for this provider

# Development

## System Requirements

The Crowd User Storage Library is developed using Java 8 (Java SDK 1.8) and Maven (Maven 3.5+).

## Building

To build this provider run the following maven command:

```
mvn clean package
```

## Implemented [capability interfaces](https://www.keycloak.org/docs/latest/server_development/#provider-capability-interfaces)

The following interfaces have been implemented:

* `UserLookupProvider`: basic user lookup (id, username, email)
* `UserQueryProvider`: complex queries that are used to locate one or more users
* `CredentialInputValidator`: validate CredentialInput, i.e. verify a password

The following interfaces will follow in the future:

* `CredentialInputUpdater`: credential type and update handling
* `UserRegistrationProvider`: adding and removing users

## Todo

- [x] Implement `UserLookupProvider`
- [x] Implement `UserQueryProvider`
- [x] Implement `CredentialInputValidator`

- [ ] Add `EditMode.UNSYNCED` support (updates are stored locally)
- [ ] Add `EditMode.WRITABLE` support (updates are stored in Crowd)
- [ ] Add user import/synchronization support (users, groups and their respective attributes are copied to Keycloak)
- [ ] Implement `CredentialInputUpdater` (allow updating credentials in Crowd)
- [ ] Implement `UserRegistrationProvider` (allow creating and removing users in Crowd)