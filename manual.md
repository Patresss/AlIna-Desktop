# AlIna Manual

## First Start

Before launching AlIna for the first time, make sure these dependencies are installed:

- JDK 21+ with `jpackage` available.
- `gcloud` CLI if you want to use Google Calendar integration.
- `googleworkspace-cli` if you want to use Google Calendar integration.

Run the app with:

```bash
./gradlew run
```

## API Key

Before first use, configure your AI provider API key, for example:

```bash
export SPRING_AI_OPENAI_API_KEY=<your_key>
```

You can also keep local overrides in `config/application.local.yml`.

## Google Calendar Setup

Google Calendar in AlIna needs both of these CLIs:

- `gcloud` for authentication.
- `gws` from `googleworkspace-cli` for calendar commands.

Install them with Homebrew:

```bash
brew install --cask google-cloud-sdk
brew install googleworkspace-cli
```

Authenticate `gcloud`:

```bash
gcloud auth application-default login
```

If Google blocks the default `gcloud` client or says the requested scopes require your own client ID, use a Desktop OAuth client JSON from Google Cloud Console:

```bash
gcloud auth application-default login --client-id-file="/path/to/client_secret.json" --scopes="https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/calendar.readonly"
```

This is required when Google shows errors like:

- `This app is blocked`
- `The following scopes will be blocked soon for the default client ID`
- `https://www.googleapis.com/auth/cloud-platform scope is required but not requested`

To prepare that file:

- create a Google Cloud project
- enable Google Calendar API
- configure the OAuth consent screen
- create an `OAuth client ID` of type `Desktop app`
- download the client JSON and keep it outside the repository

Check if both commands are available:

```bash
which gcloud
which gws
gws --help
```

## Important: Wrong `gws` Package Conflict

Homebrew also has a different package named `gws`.
That package is not the Google Workspace CLI and will not work with AlIna calendar integration.

Wrong package:

```bash
brew install gws
```

Correct package:

```bash
brew install googleworkspace-cli
```

If you installed the wrong one, replace it with:

```bash
brew uninstall gws
brew install googleworkspace-cli
rehash
```

## How To Verify The Correct `gws`

This command should describe Google Workspace features like Calendar, Drive, Gmail, Sheets, Docs, or Chat:

```bash
gws --help
```

If it talks about managing Git workspaces or repositories, you have the wrong binary installed.

## Troubleshooting Google Calendar

If AlIna says `gcloud` is missing:

- install Google Cloud SDK
- make sure the app can see `gcloud` in `PATH`

If AlIna says `gws` is missing:

- install `googleworkspace-cli`
- make sure the app can see `gws` in `PATH`

If AlIna says that `gws` is not the Google Workspace CLI:

- remove the wrong Homebrew `gws` package
- install `googleworkspace-cli`

If Google blocks login in the browser during `gcloud auth application-default login`:

- use your own Desktop OAuth client JSON with `--client-id-file`
- include both scopes: `cloud-platform` and `calendar.readonly`
- if this is a company Google Workspace account, your administrator may still block the flow

After changing CLI tools, restart AlIna.
