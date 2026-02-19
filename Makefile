all:
	sudo rm -rf target
	cd cli-tool; rm -rf target; sh install.sh

clean:
	sudo rm -rf target