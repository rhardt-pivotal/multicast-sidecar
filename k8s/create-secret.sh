#!/usr/bin/env bash
kubectl create configmap propagation-sidecar --from-file=application.yml
#kubectl create secret generic propagation-sidecar-secret \
#  --from-literal=io.hardt.propagationsidecar.jwtToken=fromRealSecret
