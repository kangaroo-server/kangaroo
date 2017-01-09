#
# Convenience Makefile, by which we can simulate the travis build environment.
#

TAG ?= latest
IMAGE ?= kangaroo/travis-toolbox

build:
	docker build -t $(IMAGE):$(TAG) --file ./tools/travis/Dockerfile .

travis: build
	docker run -ti -v `pwd`:/home/travis/build/kangaroo-server/kangaroo -v ~/.m2/repository/:/home/travis/.m2/repository/ $(IMAGE):$(TAG)
