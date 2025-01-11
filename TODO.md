
## Notes
I have not been able to get OAuth 2.0 authentication with Google to work in Postman in the production environment. Therefore, to test the REST API in production, I have created these scripts that can be run from the browser

## CREATE CUSTOMER

const url = "http://ec2-16-170-235-58.eu-north-1.compute.amazonaws.com:8080/api/customers";

const formData = new FormData();
formData.append("name", "John");
formData.append("surname", "Doe");
formData.append("email", "john45.doe@example.com");

const fileInput = document.createElement("input");
fileInput.type = "file";

fileInput.onchange = async () => {
    const file = fileInput.files[0];

    if (file) {
        formData.append("photo", file);

        try {
            const response = await fetch(url, {
                method: "POST",
                body: formData,
            });

            if (response.ok) {
                const data = await response.json();
                console.log("Cliente creado:", data);
            } else {
                const error = await response.text();
                console.error("Error al crear el cliente:", error);
            }
        } catch (err) {
            console.error("Error en la solicitud:", err);
        }
    } else {
        console.error("No se seleccionó ningún archivo.");
    }
};

fileInput.click();

----------------------------------------------------

## GET CUSTOMER DETAIL

const apiUrl =`http://ec2-16-170-235-58.eu-north-1.compute.amazonaws.com:8080/api/customers/0e51f71e-d660-4a05-9688-9ef80ffbde24`;  

// Realiza la llamada GET
fetch(apiUrl)
  .then(response => {
    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }
    return response.json();
  })
  .then(data => {
    // Aquí manejas los datos obtenidos
    console.log("Customer details:", data);
    if (data.photo) {
      console.log("Customer's photo URL:", data.photo); 
    }
  })
  .catch(error => {
    // Maneja cualquier error que ocurra durante la solicitud
    console.error("Error fetching customer details:", error);
  });


--------------------------------------------------------------

## UPDATE CUSTOMER

const url = "http://ec2-13-60-95-174.eu-north-1.compute.amazonaws.com:8080/api/customers/0e51f71e-d660-4a05-9688-9ef80ffbde24";

// Crear FormData para enviar los datos de actualización
const formData = new FormData();
formData.append("name", "Juanita");
formData.append("surname", "Reina");
formData.append("email", "juanitaReina@example.com");

// Crear un input de archivo para seleccionar la foto (opcional)
const fileInput = document.createElement("input");
fileInput.type = "file";

// Evento que se activa cuando el usuario selecciona un archivo
fileInput.onchange = async () => {
    const file = fileInput.files[0];

    if (file) {
        // Agregar la foto al FormData
        formData.append("photo", file);

        try {
            // Realizar la solicitud PATCH con fetch
            const response = await fetch(url, {
                method: "PATCH",  // Utilizamos PATCH en lugar de POST
                body: formData,
                headers: {
                    //'Authorization': 'Bearer your-token-here', // Si es necesario agregar el token
                },
            });

            // Manejar la respuesta de la API
            if (response.ok) {
                const data = await response.json();
                console.log("Cliente actualizado:", data);
            } else {
                const error = await response.text();
                console.error("Error al actualizar el cliente:", error);
            }
        } catch (err) {
            console.error("Error en la solicitud:", err);
        }
    } else {
        console.error("No se seleccionó ningún archivo.");
    }
};

// Activar el input de archivo
fileInput.click();

------------------------------------------------
## CREATE USER

fetch("http://ec2-16-170-235-58.eu-north-1.compute.amazonaws.com:8080/api/users", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({ 
		"name": "Pepe Mateo", 
		"email": "pepemateo@gmail.com",
		"isADmin": false
    })
})
.then(response => response.json())
.then(data => console.log("Respuesta del servidor:", data))
.catch(error => console.error("Error:", error));

## UPDATE USER

fetch("http://ec2-16-170-235-58.eu-north-1.compute.amazonaws.com:8080/api/users/2", {
    method: "PUT",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({ 
		"email": "gomez.delgado@toptal.com",
		"name": "toptal",
        "isAdmin": false,
        "isDeleted": false
    })
})
.then(response => response.json())
.then(data => console.log("Respuesta del servidor:", data))
.catch(error => console.error("Error:", error));


-------------------------------
## DELETE USER

fetch("http://ec2-16-170-235-58.eu-north-1.compute.amazonaws.com:8080/api/users?id=12345&email=user@example.com", {
    method: "DELETE",
    headers: {
        "Content-Type": "application/json"
    }
})
.then(response => response.json())
.then(data => console.log("Respuesta del servidor:", data))
.catch(error => console.error("Error:", error));
