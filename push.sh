#!/bin/bash
pushd src/main/java/nl/infcomtec/advswing
git pull
git add .
git commit -m"Saved"
git push
popd
pushd src/main/java/nl/infcomtec/datapond
git pull
git add .
git commit -m"Saved"
git push
popd
pushd src/main/java/nl/infcomtec/simpleimage
git pull
git add .
git commit -m"Saved"
git push
popd
git pull
git add .
git commit -m"Saved"
git push
