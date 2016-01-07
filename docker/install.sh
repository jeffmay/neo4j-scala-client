#!/usr/bin/env bash

# Setup variables
if [[ "$DOCKER_ENV" == "" ]]; then
    export DOCKER_ENV="default"
fi

echo "Using docker-machine environment '$DOCKER_ENV'"

# Verify that docker-machine is installed
hash docker-machine &> /dev/null || {
    echo "docker-machine is required. Use the link below to download Docker Toolbox."
    echo "https://www.docker.com/docker-toolbox"
    exit 1
}

# Check if environment already exists
if [[ "$(docker-machine ls | grep "$DOCKER_ENV")" != "" ]]; then
    echo "docker-machine env '$DOCKER_ENV' already exists!"
    # TODO Offer to reinstall or enable it
    exit 1
fi

OPTS=$@

function printUsage() {
    echo "Usage: $0 -d | --driver DOCKER_DRIVER [-v | --version NEO4J_VERSION]"
    echo ""
    echo "    -d, --driver (REQUIRED):"
    echo "        The docker-machine driver (ex 'virtualbox') to use to create the '$DOCKER_ENV' env"
    echo "    -v, --version (default: 'latest'):"
    echo "        The version of the neo4j image to pull"
    echo ""
}

DOCKER_DRIVER=""
NEO4J_VERSION="latest"

while [[ $# > 0 ]]
do
key="$1"

case $key in

    -d|--driver)
        DOCKER_DRIVER="$2"
        shift
        if [[ "$DOCKER_DRIVER" == "" ]]; then
            echo "Empty argument for $key"
            printUsage
            exit 1
        fi
    ;;

    -v|--version)
        NEO4J_VERSION="$2"
        shift
        if [[ "$NEO4J_VERSION" == "" ]]; then
            echo "Empty argument for $key"
            printUsage
            exit 1
        fi
    ;;

    *)
        echo "Unknown option: $OPTS"
        echo ""
        printUsage
        exit 1
    ;;

esac
shift
done

# Verify that the driver is provided
if [[ "$DOCKER_DRIVER" == "" ]]; then
    echo "Argument -d | --driver is required. Please see options from docker-machine:"
    echo ""
    echo $(docker-machine create --help | grep "\-\-driver")
    exit 1
fi

# Create the docker machine environment
docker-machine create -d ${DOCKER_DRIVER} ${DOCKER_ENV}

# Set the local environment
eval "$(docker-machine env ${DOCKER_ENV})"

# Get the latest docker image for neo4j
docker pull neo4j:${NEO4J_VERSION}
