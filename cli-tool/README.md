# GWT Boot CLI (Java Version)

This is a Java rewrite of the original bash `gwt-boot` script. It provides the same functionality as a cross-platform Java application using picocli for command-line parsing.

## Features

- **boot**: Generate a new GWT Boot project from archetype
- **activity**: Create a new activity in the current project
- **service**: Create a new GWT RPC service in the current project

## Building

```bash
mvn clean package
```

This will create an executable JAR at `target/gwt-boot-cli.jar`.

## Running

### Using the launcher script (Unix/macOS/Linux):

```bash
./gwt-boot boot myapp
./gwt-boot activity dashboard
./gwt-boot service UserAuth
```

### Using Java directly:

```bash
java -jar target/gwt-boot-cli.jar boot myapp
java -jar target/gwt-boot-cli.jar activity dashboard
java -jar target/gwt-boot-cli.jar service UserAuth
```

## Installation

The CLI tool can be installed globally for easy access from anywhere on your system.

### Linux / macOS

#### Quick Install (from URL)

If the files are hosted on a server, you can install directly:

```bash
# Download and run install script
curl -fsSL https://your-server.com/path/to/install.sh | bash
```

Or download both files first:

```bash
# Download JAR and install script
wget https://your-server.com/path/to/gwt-boot-cli.jar
wget https://your-server.com/path/to/install.sh
chmod +x gwt-boot
./gwt-boot
```

#### Local Install

If you've built the project locally:

```bash
# Build the project first
mvn clean package

# Copy JAR to cli-tool directory
cp target/gwt-boot-cli.jar .

# Run install script
./gwt-boot
```

The install script will:
- Detect if you have sudo privileges
- Offer to install system-wide to `/usr/local/lib/gwt-boot/` and `/usr/local/bin/` (requires sudo)
- Otherwise install to `~/.local/lib/gwt-boot/` and `~/.local/bin/` (user-local)
- Create an executable wrapper script for easy access

If installed to user-local directory, you may need to add to your PATH:

```bash
# For bash
echo 'export PATH="$PATH:$HOME/.local/bin"' >> ~/.bashrc
source ~/.bashrc

# For zsh
echo 'export PATH="$PATH:$HOME/.local/bin"' >> ~/.zshrc
source ~/.zshrc
```

### Windows

```batch
install.bat
```

This will:
- Install the GWT Boot archetype to your local Maven repository (`%USERPROFILE%\.m2\repository\`)
- Build the CLI JAR file
- Install JAR to `%USERPROFILE%\AppData\Local\gwt-boot\lib\`
- Create wrapper scripts (`gwt-boot.bat` and `gwt-boot.ps1`) in `%USERPROFILE%\AppData\Local\gwt-boot\bin\`

Add to your PATH permanently (run as Administrator):

```batch
setx PATH "%PATH%;%USERPROFILE%\AppData\Local\gwt-boot\bin"
```

Or temporarily for current session:

```batch
set PATH=%PATH%;%USERPROFILE%\AppData\Local\gwt-boot\bin
```

### Verify Installation

```bash
gwt-boot version
```

Now you can use `gwt-boot` from anywhere:

```bash
gwt-boot boot myapp
gwt-boot activity dashboard
gwt-boot service UserAuth
```

### Uninstalling

To uninstall GWT Boot CLI from your system:

```bash
gwt-boot uninstall
```

This will:
- Detect all installed instances (system-wide and user-local)
- Show which files will be removed
- Ask for confirmation before deleting
- Remove the JAR file and wrapper scripts

You can skip the confirmation prompt with:

```bash
gwt-boot uninstall -y
```

## Commands

### boot [artifactId]

Generate a new GWT Boot project from archetype.

```bash
gwt-boot boot myapp
```

Interactive prompts will ask for:
- groupId (e.g., com.mycompany)
- artifactId (if not provided as argument)
- version (default: 0.0.1-SNAPSHOT)
- package (default: groupId.artifactId)

### activity <name>

Create a new activity in the current project.

```bash
gwt-boot activity dashboard
```

Creates:
- Presenter class
- View class with UiBinder
- GWT module file
- Spring Boot controller
- Thymeleaf template

### service <name>

Create a new GWT RPC service in the current project.

```bash
gwt-boot service UserAuth
```

Creates:
- Service interface
- Async service interface
- Service implementation

### uninstall

Uninstall GWT Boot CLI from your system.

```bash
gwt-boot uninstall
```

Options:
- `-y, --yes`: Skip confirmation prompt

This command will detect and remove all installed instances of the CLI tool from your system, including both system-wide and user-local installations.

## Requirements

- Java 21 or higher
- Maven (for building and for the `boot` command)

## Advantages over Bash Script

1. **Cross-platform**: Works on Windows, macOS, Linux without modification
2. **Type safety**: Compile-time checking for errors
3. **Better error handling**: Structured exception handling
4. **Easier testing**: Can write unit tests for each command
5. **More maintainable**: Object-oriented design with clear separation of concerns
6. **IDE support**: Full IDE support with auto-completion and refactoring
7. **Better argument parsing**: Using picocli for robust CLI parsing

## Project Structure

```
cli-tool/
├── pom.xml
├── gwt-boot.sh (launcher script)
├── README.md
└── src/main/java/com/edusoftwerks/gwtboot/cli/
    ├── GwtBootCli.java          (Main class)
    ├── Console.java             (Colored console output)
    ├── InputReader.java         (User input handling)
    ├── ProcessExecutor.java     (Process execution utilities)
    ├── PomUtils.java           (POM XML parsing)
    ├── BootCommand.java         (boot command)
    ├── ActivityCommand.java     (activity command)
    └── ServiceCommand.java      (service command)
```

## License

Same license as the GWT Boot Archetype project.
