# 🚀 J3D - Java Software Renderer

O **J3D** é um motor de renderização 3D de alta performance processado inteiramente via software. Ao contrário de engines modernas que dependem de GPUs e APIs como OpenGL ou Vulkan, o J3D realiza todos os cálculos matemáticos de álgebra linear, projeção de perspectiva e rasterização de pixels diretamente na **CPU** utilizando **Java puro**.

## 🏗️ Arquitetura Modular

O projeto segue padrões de design modulares para facilitar a manutenção e expansão:

* **`core`**: O núcleo da aplicação. Contém o `Launcher` (ponto de entrada) e o `GameObject`, que encapsula a lógica dos objetos.
* **`render`**: Gerencia o pipeline de renderização (`SoftwareRenderer`), a `Camera`, o loop principal e os buffers (Imagem e Z-Buffer).
* **`math`**: Biblioteca matemática contendo `Matrix4` e `Transform` para cálculos de álgebra linear e transformações.
* **`geometry`**: Define as formas geométricas (`Mesh`) e primitivas de construção (`Vertex`, `Triangle`).
* **`lighting`**: Gerencia fontes de luz e cálculos de iluminação (`PointLight`).

## 🛠️ Destaques Técnicos

### 1. Sistema de Target FPS
O motor possui um controle dinâmico de taxa de quadros (`TARGET_FPS`). Implementamos um fator de correção (`speedCorrection`) que normaliza o movimento com base em um padrão de 60 FPS (16ms), garantindo consistência física independente da fluidez visual.

### 2. Iluminação Lambertiana
Utilizamos sombreamento difuso para calcular a interação da luz com as faces:
* **Atenuação de Distância**: A intensidade da luz diminui conforme se afasta da fonte.
* **Produto Escalar (Dot Product)**: Determina o brilho com base no ângulo entre a normal da face e o vetor da luz.

### 3. Z-Buffering (Depth Buffer)
Um buffer de profundidade armazena a distância de cada pixel, resolvendo o problema de visibilidade e garantindo que objetos próximos cubram corretamente os distantes.

## 🎮 Comandos do Laboratório

| Categoria | Teclas | Função |
| :--- | :--- | :--- |
| **Movimento** | `W`, `S`, `A`, `D` | Navegação FPS (Frente, Trás, Strafe) |
| **Visão** | `Mouse` | Olhar ao redor (Yaw e Pitch) |
| **Luz (X, Z)** | `Setas Direcionais` | Move o spot de luz pelo plano horizontal |
| **Luz (Y)** | `I` / `K` | Sobe ou desce o spot de luz |
| **Modos** | `F2` | Alterna entre Preenchimento Sólido e **Wireframe** |
| **Gizmo** | `F3` | Mostra/Esconde a esfera da luz |

## 🚀 Como Executar

1. Compile a partir da pasta `src`:
   `javac core/*.java geometry/*.java lighting/*.java math/*.java render/*.java`
2. Execute:
   `java core.Launcher`

---
Desenvolvido como um laboratório de computação gráfica via Software.
