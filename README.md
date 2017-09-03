# Kangaroo: An OAuth2 Server
[![Build Status](https://travis-ci.org/kangaroo-server/kangaroo.svg)](https://travis-ci.org/kangaroo-server/kangaroo)
[![codecov](https://codecov.io/gh/kangaroo-server/kangaroo/branch/develop/graph/badge.svg)](https://codecov.io/gh/kangaroo-server/kangaroo)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo)

## Why?
* Because nobody can agree on what OAuth2 is.
* Because I'm sick and tired of starting a new project and having to implement an auth layer again.

## Design decisions
* This server is for authorization, and LIMITED authentication.
  We really don't want to store passwords.
* Jersey2 as the JAX-RS implementation, because it's what I'm familiar with.
* Injection framework: HK2 (since it comes with Jersey2)
* Jackson to deserialize things.
* Identity is managed separately from users. Identities, and the identity 
  claims made by an authentication provider, should adhere to the T&C of the 
  authenticating API (for instance, Facebook is stringent about caching data).

## Things that aren't in the spec.
* How do we register users that aren't implicitly provided by something like google?
* How would a third party service validate that a token it was handed is valid?
* Should we bother layering on something like OpenID Connect which can return JWT user data claims?

## RFC's to implement