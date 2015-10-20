#!/bin/bash
for i in {1..100}; do
  curl -X POST -H "Content-Type: application/json" -uroot:gtn http://plfent-4.3.x-pkgpriv-responsive-design-snapshot.acceptance6.exoplatform.org/rest/private/v1/social/users/root/activities -d '{"title":"inject some data"}';
done
