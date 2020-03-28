#!/bin/bash

function tag_and_push() {
  docker tag samschmit/keycloak-crowd samschmit/keycloak-crowd:"$1"
  docker push samschmit/keycloak-crowd:"$1"
}

function conditionally_tag_and_push() {
  local pattern=$1
  local version=$2
  local tag=$3

  latest=$(git branch --list -r "origin/release/$pattern" | cut -d "/" -f3 | sort -r | head -n 1)
  if [ "$version" == "$latest" ]; then
    tag_and_push "$tag"
  fi
}

current_major_version=$(echo "$PROJECT_VERSION" | cut -d "." -f1)
current_major_minor_version=$(echo "$PROJECT_VERSION" | cut -d "." -f1-2)

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker build -t samschmit/keycloak-crowd .

tag_and_push "$current_major_minor_version"
conditionally_tag_and_push "$current_major_version.*" "$current_major_minor_version" "$current_major_version"
conditionally_tag_and_push "*" "$current_major_minor_version" "latest"
