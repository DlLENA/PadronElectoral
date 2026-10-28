
document.addEventListener("DOMContentLoaded", () => {
    // 1. PANTALLA DE REGISTRO
    const formRegistro = document.getElementById('formRegistro');
    if (formRegistro) {
        cargarTodosLosCatalogos();
        cargarCaptchaRegistro(); 
        formRegistro.addEventListener('submit', realizarRegistro);
    }

    // 2. PANTALLA DE CONSULTA
    const formConsulta = document.getElementById('formConsulta');
    if (formConsulta) {
        cargarCaptchaConsulta(); 
        formConsulta.addEventListener('submit', realizarConsulta);
    }
});

// ==========================================
// FUNCIONES DE REGISTRO
// ==========================================
function cargarTodosLosCatalogos() {
    // Función reutilizable para llenar los <select>
    const llenarSelect = (url, selectIds) => {
        fetch(`/api/catalogos/${url}`)
            .then(res => res.json())
            .then(data => {
                let opciones = '<option value="">Seleccione...</option>';
                data.forEach(item => opciones += `<option value="${item.id}">${item.nombre || item.descripcion}</option>`);
                selectIds.forEach(id => document.getElementById(id).innerHTML = opciones);
            }).catch(e => console.error(`Error cargando ${url}:`, e));
    };

    llenarSelect('municipios', ['idMuniVecindad', 'idMuniResidencia']);
    llenarSelect('generos', ['idGenero']);
    llenarSelect('estados', ['idEstado']);
    llenarSelect('afiliaciones', ['idAfiliacion']);
}

function cargarCaptchaRegistro() {
    fetch('/api/captcha')
        .then(response => response.json())
        .then(data => {
            const img = document.getElementById('imagenCaptchaReg');
            img.src = data.imagenBase64.startsWith('data:image') ? data.imagenBase64 : 'data:image/png;base64,' + data.imagenBase64;
            img.style.display = 'inline-block';
            document.getElementById('captchaIdOcultoReg').value = data.id; 
            document.getElementById('respuestaCaptchaReg').value = ''; 
        }).catch(err => console.error(err));
}

function realizarRegistro(event) {
    event.preventDefault(); 
    mostrarAlertaRegistro('Procesando registro en base de datos...', 'info');

    const payload = {
        cui: document.getElementById('cui').value.trim(),
        fechaEmisionDpi: document.getElementById('fechaEmisionDpi').value,
        fechaNacimiento: document.getElementById('fechaNacimiento').value,
        primerNombre: document.getElementById('primerNombre').value.trim(),
        segundoNombre: document.getElementById('segundoNombre').value.trim(),
        apellidos: document.getElementById('apellidos').value.trim(),
        
        leeEscribe: document.getElementById('leeEscribe').value === 'true',
        vidente: document.getElementById('vidente').value === 'true',
        tieneDireccionGeoelectoral: document.getElementById('tieneDireccionGeoelectoral').value === 'true',
        
        idGenero: parseInt(document.getElementById('idGenero').value),
        idEstado: parseInt(document.getElementById('idEstado').value),
        idAfiliacion: parseInt(document.getElementById('idAfiliacion').value),
        idMuniVecindad: parseInt(document.getElementById('idMuniVecindad').value),
        idMesa: parseInt(document.getElementById('idMesa').value),
        idMuniResidencia: parseInt(document.getElementById('idMuniResidencia').value),
        
        captchaId: document.getElementById('captchaIdOcultoReg').value,
        captchaRespuesta: document.getElementById('respuestaCaptchaReg').value
    };

    fetch('/api/empadronamientos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(async response => {
        if (response.ok || response.status === 201) { 
			mostrarAlertaRegistro(`✅ REGISTRO EXITOSO. Su No. de Empadronamiento es: <strong>${successData.numeroEmpadronamiento}</strong>`, 'success');
            document.getElementById('formRegistro').reset(); 
            cargarCaptchaRegistro(); 
        } else {
            const errorData = await response.json();
            mostrarAlertaRegistro(`❌ Error de validación: ${errorData.mensaje || 'Datos incorrectos'}`, 'danger');
            cargarCaptchaRegistro(); 
        }
    })
    .catch(error => mostrarAlertaRegistro('❌ Error crítico de red.', 'danger'));
}

function mostrarAlertaRegistro(mensaje, tipo) {
    const divAlerta = document.getElementById('mensajeAlerta');
    divAlerta.innerHTML = `<div class="alert alert-${tipo} fw-bold">${mensaje}</div>`;
    divAlerta.style.display = 'block';
}

// ==========================================
// FUNCIONES DE CONSULTA 
// ==========================================
function cargarCaptchaConsulta() {
    fetch('/api/captcha')
        .then(response => response.json())
        .then(data => {
            const img = document.getElementById('imagenCaptcha');
            img.src = data.imagenBase64.startsWith('data:image') ? data.imagenBase64 : 'data:image/png;base64,' + data.imagenBase64;
            img.style.display = 'inline-block';
            document.getElementById('captchaIdOculto').value = data.id; 
            document.getElementById('respuestaCaptcha').value = ''; 
        }).catch(err => console.error(err));
}

function realizarConsulta(event) {
    event.preventDefault(); 
    const payload = {
		tipo: document.getElementById('tipoBusqueda').value,
        numero: document.getElementById('dpiConsulta').value,
        fechaNacimiento: document.getElementById('fechaNacConsulta').value,
        captchaId: document.getElementById('captchaIdOculto').value,
        captchaRespuesta: document.getElementById('respuestaCaptcha').value
    };

    fetch('/api/consultas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(async response => {
        const divResultado = document.getElementById('resultadoConsulta');
        divResultado.style.display = 'block';
        
        if (response.ok) {
            const ciudadano = await response.json(); 
			divResultado.innerHTML = `
			                <div class="card border-success mb-3 mt-3 shadow-sm">
			                    <div class="card-header bg-success text-white fw-bold">
			                        ✅ Estado: ${ciudadano.estado}
			                    </div>
			                    <div class="card-body">
			                        <!-- 1. IDENTIFICACIÓN -->
			                        <h6 class="fw-bold text-success border-bottom pb-1">🪪 Identificación</h6>
			                        <p class="card-text mb-1"><strong>No. Empadronamiento:</strong> ${ciudadano.identificacion.numeroEmpadronamiento || 'N/A'}</p>
			                        <p class="card-text mb-1"><strong>DPI (CUI):</strong> ${ciudadano.identificacion.cui}</p>
			                        <p class="card-text mb-1"><strong>Fecha de Emisión:</strong> ${ciudadano.identificacion.fechaEmisionDpi || 'N/A'}</p>
			                        
			                        <!-- 2. DATOS PERSONALES -->
			                        <h6 class="fw-bold text-success border-bottom pb-1 mt-3">👤 Datos Personales</h6>
			                        <p class="card-text mb-1"><strong>Nombre Completo:</strong> ${ciudadano.datosPersonales.nombreCompleto}</p>
			                        <p class="card-text mb-1"><strong>Género:</strong> ${ciudadano.datosPersonales.genero || 'N/A'}</p>
			                        <p class="card-text mb-1"><strong>¿Lee y escribe?:</strong> ${ciudadano.datosPersonales.leeEscribe || 'N/A'}</p>
			                        <p class="card-text mb-1"><strong>¿Es vidente?:</strong> ${ciudadano.datosPersonales.vidente || 'N/A'}</p>
			                        
			                        <!-- 3. VECINDAD -->
			                        <h6 class="fw-bold text-success border-bottom pb-1 mt-3">📍 Vecindad (DPI)</h6>
			                        <p class="card-text mb-1"><strong>Departamento:</strong> ${ciudadano.vecindad.departamento}</p>
			                        <p class="card-text mb-1"><strong>Municipio:</strong> ${ciudadano.vecindad.municipio}</p>
			                        
			                        <!-- 4. RESIDENCIA Y VOTACIÓN -->
			                        <h6 class="fw-bold text-success border-bottom pb-1 mt-3">🗳️ Residencia y Votación</h6>
			                        <p class="card-text mb-1"><strong>Municipio de Residencia:</strong> ${ciudadano.residenciaVotacion.municipioResidencia || 'N/A'}</p>
			                        <p class="card-text mb-1"><strong>Dirección Geoelectoral:</strong> ${ciudadano.residenciaVotacion.direccionGeoelectoral || 'N/A'}</p>
			                        <p class="card-text mb-1"><strong>Centro de Votación:</strong> ${ciudadano.residenciaVotacion.centroVotacion}</p>
			                        <p class="card-text mb-1 text-danger fw-bold"><strong>Mesa Asignada:</strong> ${ciudadano.residenciaVotacion.numeroMesa}</p>
			                        <p class="card-text mb-1"><strong>Afiliación:</strong> ${ciudadano.residenciaVotacion.afiliacion || 'N/A'}</p>
			                    </div>
			                </div>`;
        } else {
            const errorData = await response.json();
            divResultado.innerHTML = `<div class="alert alert-danger"><strong>❌ ${errorData.mensaje || 'DPI no encontrado o código incorrecto.'}</strong></div>`;
            cargarCaptchaConsulta(); 
        }
    })
    .catch(error => {
        document.getElementById('resultadoConsulta').innerHTML = `<div class="alert alert-danger"><strong>Error de conexión.</strong></div>`;
        document.getElementById('resultadoConsulta').style.display = 'block';
    });
}