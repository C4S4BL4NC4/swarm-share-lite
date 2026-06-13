# Create the root project folder
#mkdir -p swarm-share-lite
#cd swarm-share-lite

# NEEDED BECAUSE GITHUB DOESN'T TRACK EMPTY FOLDER -

# Create the config folder
mkdir -p config

# Create core module directories
mkdir -p core/src/main/java/io/swarmshare/core/domain
mkdir -p core/src/main/java/io/swarmshare/core/port
mkdir -p core/src/test/java/io/swarmshare/core/domain

# Create manifest module directories
mkdir -p manifest/src/main/java/io/swarmshare/manifest
mkdir -p manifest/src/test/java/io/swarmshare/manifest

# Create storage module directories
mkdir -p storage/src/main/java/io/swarmshare/storage
mkdir -p storage/src/test/java/io/swarmshare/storage

# Create networking module directories
mkdir -p networking/src/main/java/io/swarmshare/networking
mkdir -p networking/src/test/java/io/swarmshare/networking

# Create transfer module directories
mkdir -p transfer/src/main/java/io/swarmshare/transfer
mkdir -p transfer/src/test/java/io/swarmshare/transfer

# Create cli module directories
mkdir -p cli/src/main/java/io/swarmshare/cli