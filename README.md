# Blackout

Blackout is a distributed load testing infrastructure for the
Orchestra SaaS platform.  We have ambitions to make some of the
facilities of this available as a file-driven DSL so it's easy to
build and customize test infrastructure.

This library is intended to be used both as a local test runner and a
master node for distributed testing.  Simulated load tests are
generated by clj-gatling.

To observe system behavior under test, possibly customized to the
application in questions, we leverage Reimann, a high performance
stream-oriented statistics aggregator.

Use of Reimann for real-time charting and reporting is optional,
allowing blackout to be used as a standlone test platform against a
co-located server, such as in automated build and test.

TODO: We leverage Clojurecast to facilitate a, relatively simple,
distributed version of this framework to enable aggregating load data
across a set of child nodes

## Block Diagram

Single node testing

System Under Test <-> Test Runner -> Report -> Reimann Server -> Reimann Dashboard

Cluster-based testing

Cluster Under Test <-> Test Runner(s) -> Dist Reports -> Reimann Server -> Reimann Dash

Hack clj-gatling with custom reporter to aggregate statistics on a
master node.  Streaming to Reimann is distributed.

## Pre-requisites for local testing

1. Install Riemann and associated tools

When using Mac OS X, issue the following commands at a terminal: 

a. `brew install riemann`

b. `sudo gem install riemann-client riemann-tools riemann-dash`

c. `riemann /usr/local/etc/riemann.config` 

d. `sudo riemann-dash` (sudo is necessary to permit writes for config saves)

e. Open your web browser to http://localhost:4567

2. Launch REPL with 'lein repl'

3. At the repl, kick off a run...


## Pre-requisites for distributed testing

TODO

## Roadmap

- How to make load testing more generic?
- Use clojurecast to allow distributed test generators to coordinate, e.g. unique name generation.
- Use spec.test and/or simulant to generate test cases against specific APIs
- Ansible script for configuring a test node to run riemann, riemann
dash, blackout from a JAR and connect to upstream Reimann server
- Ansible script for creating a cluster of test nodes
- Parse a simple DSL for trivial testing based on a file located on
  the classpath, e.g. using CURL styel commands for individual steps
  with a simple variable substition model

## Copyright and License

Copyright © 2016 Vital Labs, Inc.

Released under the MIT License

