#!/usr/bin/env bash

# Shared by generation and packaging. A package carries data, never shell code.
read_starter_provenance() {
  local source_root=$1 supplied_commit=${2:-} key value source_changes seen=" "
  [[ -z "$supplied_commit" || "$supplied_commit" =~ ^[0-9a-f]{40}$ || "$supplied_commit" =~ ^[0-9a-f]{64}$ ]] || return 1
  starter_commit="unknown"
  starter_version="unknown"
  starter_worktree="unknown"
  if [[ -e "$source_root/STARTER_SOURCE" || -L "$source_root/STARTER_SOURCE" ]]; then
    [[ -f "$source_root/STARTER_SOURCE" && ! -L "$source_root/STARTER_SOURCE" ]] || return 1
    while IFS='=' read -r key value || [[ -n "$key" ]]; do
      [[ "$seen" != *" $key "* ]] || return 1
      case "$key" in
        commit)
          [[ "$value" == unknown || "$value" =~ ^[0-9a-f]{40}$ || "$value" =~ ^[0-9a-f]{64}$ ]] || return 1
          starter_commit=$value ;;
        version)
          [[ "$value" == unknown || "$value" == unreleased || "$value" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || return 1
          starter_version=$value ;;
        worktree)
          case "$value" in clean|modified|unknown) ;; *) return 1 ;; esac
          starter_worktree=$value ;;
        *) return 1 ;;
      esac
      seen+="$key "
    done < "$source_root/STARTER_SOURCE"
    [[ "$seen" == *" commit "* && "$seen" == *" version "* && "$seen" == *" worktree "* ]] || return 1
  elif [[ "$(git -C "$source_root" rev-parse --show-toplevel 2>/dev/null || true)" == "$source_root" ]] \
      && starter_commit=$(git -C "$source_root" rev-parse --verify HEAD 2>/dev/null); then
    starter_version="unreleased"
    starter_worktree="modified"
    source_changes=$(git -C "$source_root" status --porcelain --untracked-files=normal) || return 1
    if [[ -z "$source_changes" ]]; then
      starter_worktree="clean"
      while IFS= read -r value; do
        if [[ "$value" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then starter_version=$value; break; fi
      done < <(git -C "$source_root" tag --points-at HEAD --list 'v*' --sort=-version:refname)
    fi
  else
    starter_commit="unknown"
  fi
  if [[ -n "$supplied_commit" ]]; then
    [[ "$starter_commit" == unknown || "$starter_commit" == "$supplied_commit" ]] || return 1
    starter_commit=$supplied_commit
  fi
}
