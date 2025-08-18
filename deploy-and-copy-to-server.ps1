# Zmienne
$dockerImageName = "alina-app"
$dockerImageTag = "latest"
$outputFilePath = "alina-app.tar"
$networkPath = "\\PatrykNas\Applications\Alina"

Write-Output "Budowanie gradle:"
gradle build

Write-Output "Buduj obraz Dockerowy:"
docker build -t "${dockerImageName}:${dockerImageTag}" ./server

# Krok 2: Sprawdź dostępne obrazy Dockerowe
Write-Output "Dostępne obrazy:"
docker images

# Krok 3: Wyeksportuj obraz do pliku tar
Write-Output "Eksportowanie obrazu do pliku tar..."
docker save -o $outputFilePath "${dockerImageName}:${dockerImageTag}"


Write-Output "Kopiowanie pliku do lokalnych udostępnionych zasobów sieciowych..."
Copy-Item -Path $outputFilePath -Destination $networkPath


Write-Output "Skrypt zakonczyl dzialanie."