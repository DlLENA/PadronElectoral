// Se ejecuta apenas la página HTML termina de cargar
document.addEventListener("DOMContentLoaded", () => {
    
    // 1. LÓGICA PARA LA PANTALLA DE REGISTRO
    const selectMunicipio = document.getElementById('municipio');
    if (selectMunicipio) {
        cargarMunicipios();
    }

    // 2. LÓGICA PARA LA PANTALLA DE CONSULTA
    const formConsulta = document.getElementById('formConsulta');
    if (formConsulta) {
        cargarCaptchaConsulta(); // Llama al CAPTCHA de Diego al entrar a la página
        formConsulta.addEventListener('submit', realizarConsulta);
    }
});

// ==========================================
// FUNCIONES DE REGISTRO
// ==========================================
function cargarMunicipios() {
    const url = 'http://localhost:8081/api/catalogos/municipios';
    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Error al conectar con la API');
            return response.json();
        })
        .then(data => {
            const selectMunicipio = document.getElementById('municipio');
            selectMunicipio.innerHTML = '<option value="">Seleccione un municipio...</option>'; 
            data.forEach(municipio => {
                const option = document.createElement('option');
                option.value = municipio.id;       
                option.text = municipio.nombre;    
                selectMunicipio.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('municipio').innerHTML = '<option value="">Error al cargar municipios</option>';
        });
}

// ==========================================
// FUNCIONES DE CONSULTA
// ==========================================
function cargarCaptchaConsulta() {
    fetch('http://localhost:8081/api/captcha')
        .then(response => response.json())
        .then(data => {
            const img = document.getElementById('imagenCaptcha');
            const hiddenId = document.getElementById('captchaIdOculto');
            
            // Verifica si el backend ya manda el prefijo de imagen, sino se lo agregamos
            if (data.imagenBase64.startsWith('data:image')) {
                img.src = data.imagenBase64;
            } else {
                img.src = 'data:image/png;base64,' + data.imagenBase64;
            }
            
            img.style.display = 'block';
            hiddenId.value = data.id; // Guardamos el ID secreto para mandarlo en el POST
            document.getElementById('respuestaCaptcha').value = ''; // Limpiar la cajita
        })
        .catch(error => {
            console.error('Error al cargar CAPTCHA:', error);
        });
}

function realizarConsulta(event) {
    event.preventDefault(); // Evita que la página parpadee y se recargue

    // Recolectar los datos del HTML
    const dpi = document.getElementById('dpiConsulta').value;
    const fechaNac = document.getElementById('fechaNacConsulta').value;
    const captchaId = document.getElementById('captchaIdOculto').value;
    const captchaResp = document.getElementById('respuestaCaptcha').value;
    const divResultado = document.getElementById('resultadoConsulta');

    // Armar el JSON exactamente como en el Swagger
    const payload = {
        tipo: "EMPADRONAMIENTO",
        numero: dpi,
        fechaNacimiento: fechaNac,
        captchaId: captchaId,
        captchaRespuesta: captchaResp
    };

    // Hacer el POST a la API
    fetch('http://localhost:8081/api/consultas', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(async response => {
        divResultado.style.display = 'block';
        
        if (response.ok) {
            // Si el backend da un código 200 OK (Empadronado)
            divResultado.innerHTML = `
                <div class="alert alert-success">
                    <strong>¡Ciudadano Empadronado!</strong><br>
                    El DPI ${dpi} se encuentra registrado en el sistema electoral.
                </div>`;
        } else {
            // Si da error (CAPTCHA inválido, DPI no encontrado, etc.)
            divResultado.innerHTML = `
                <div class="alert alert-danger">
                    <strong>Alerta:</strong> No se pudo validar el empadronamiento o el código de seguridad es incorrecto.
                </div>`;
            cargarCaptchaConsulta(); // Recargamos la imagen por seguridad
        }
    })
    .catch(error => {
        divResultado.style.display = 'block';
        divResultado.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error de conexión:</strong> No hay comunicación con el servidor.
            </div>`;
    });
}