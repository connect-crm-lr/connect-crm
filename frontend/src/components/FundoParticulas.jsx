import { useEffect, useRef } from 'react';

// Fundo animado da tela de login: partículas ligadas por linhas e atraídas pelo mouse.
// Canvas puro com requestAnimationFrame, sem bibliotecas.
const DISTANCIA_LINHA = 130; // px: duas partículas mais próximas que isso ganham uma linha
const RAIO_MOUSE = 180; // px: partículas dentro deste raio são puxadas e ligadas ao cursor
const RAIO_BRILHO = 252; // px: brilho azul em volta do cursor

const aleatorio = (min, max) => min + Math.random() * (max - min);

export default function FundoParticulas({ className }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    // Quem pede menos movimento no sistema vê um quadro parado, redesenhado só quando o mouse mexe.
    const reduzirMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const mouse = { x: 0, y: 0, ativo: false };
    let largura = 0;
    let altura = 0;
    let particulas = [];
    let quadro = null;

    function redimensionar() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2); // tela retina sem pesar demais
      largura = canvas.clientWidth;
      altura = canvas.clientHeight;
      canvas.width = largura * dpr;
      canvas.height = altura * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

      const quantidade = Math.min(140, Math.round((largura * altura) / 11000));
      particulas = Array.from({ length: quantidade }, () => ({
        x: Math.random() * largura,
        y: Math.random() * altura,
        vx: aleatorio(-0.2, 0.2),
        vy: aleatorio(-0.2, 0.2),
        r: aleatorio(1, 2.6),
      }));
    }

    function atualizar() {
      for (const p of particulas) {
        // Atração pelo mouse: quanto mais perto do cursor, mais forte.
        if (mouse.ativo) {
          const dx = mouse.x - p.x;
          const dy = mouse.y - p.y;
          const d = Math.hypot(dx, dy);
          if (d > 0 && d < RAIO_MOUSE) {
            const forca = (1 - d / RAIO_MOUSE) * 0.06;
            p.vx += (dx / d) * forca;
            p.vy += (dy / d) * forca;
          }
        }

        // Atrito, e um empurrãozinho para nenhuma partícula ficar parada.
        p.vx *= 0.985;
        p.vy *= 0.985;
        if (Math.abs(p.vx) < 0.05) p.vx += aleatorio(-0.025, 0.025);
        if (Math.abs(p.vy) < 0.05) p.vy += aleatorio(-0.025, 0.025);

        p.x += p.vx;
        p.y += p.vy;

        // Bate na borda e volta, sem sair da tela.
        if (p.x < 0) { p.x = 0; p.vx = Math.abs(p.vx); }
        if (p.x > largura) { p.x = largura; p.vx = -Math.abs(p.vx); }
        if (p.y < 0) { p.y = 0; p.vy = Math.abs(p.vy); }
        if (p.y > altura) { p.y = altura; p.vy = -Math.abs(p.vy); }
      }
    }

    function desenhar() {
      ctx.clearRect(0, 0, largura, altura);

      // 1. Brilho em volta do mouse
      if (mouse.ativo) {
        const brilho = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, RAIO_BRILHO);
        brilho.addColorStop(0, 'rgba(99, 140, 255, 0.16)');
        brilho.addColorStop(1, 'rgba(99, 140, 255, 0)');
        ctx.fillStyle = brilho;
        ctx.fillRect(0, 0, largura, altura);
      }

      // 2. Linhas entre partículas próximas (mais fracas quanto mais longe)
      ctx.lineWidth = 1;
      for (let i = 0; i < particulas.length; i++) {
        const a = particulas[i];
        for (let j = i + 1; j < particulas.length; j++) {
          const b = particulas[j];
          const d = Math.hypot(a.x - b.x, a.y - b.y);
          if (d < DISTANCIA_LINHA) {
            ctx.strokeStyle = `rgba(140, 170, 255, ${(1 - d / DISTANCIA_LINHA) * 0.35})`;
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.stroke();
          }
        }
      }

      // 3. Linhas até o mouse
      if (mouse.ativo) {
        for (const p of particulas) {
          const d = Math.hypot(mouse.x - p.x, mouse.y - p.y);
          if (d < RAIO_MOUSE) {
            ctx.strokeStyle = `rgba(170, 195, 255, ${(1 - d / RAIO_MOUSE) * 0.6})`;
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(mouse.x, mouse.y);
            ctx.stroke();
          }
        }
      }

      // 4. As partículas
      ctx.fillStyle = 'rgba(205, 220, 255, 0.85)';
      for (const p of particulas) {
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    function animar() {
      atualizar();
      desenhar();
      quadro = requestAnimationFrame(animar);
    }

    function iniciar() {
      if (quadro === null && !reduzirMovimento) quadro = requestAnimationFrame(animar);
    }

    function parar() {
      if (quadro !== null) cancelAnimationFrame(quadro);
      quadro = null;
    }

    function aoMoverMouse(evento) {
      const area = canvas.getBoundingClientRect();
      mouse.x = evento.clientX - area.left;
      mouse.y = evento.clientY - area.top;
      mouse.ativo = true;
      if (reduzirMovimento) desenhar();
    }

    function aoSairMouse() {
      mouse.ativo = false;
      if (reduzirMovimento) desenhar();
    }

    function aoRedimensionar() {
      redimensionar();
      if (reduzirMovimento) desenhar();
    }

    // Aba escondida: para a animação e não gasta processador.
    function aoMudarVisibilidade() {
      if (document.hidden) parar();
      else iniciar();
    }

    redimensionar();
    if (reduzirMovimento) desenhar();
    else iniciar();

    window.addEventListener('mousemove', aoMoverMouse);
    window.addEventListener('resize', aoRedimensionar);
    document.addEventListener('mouseleave', aoSairMouse);
    document.addEventListener('visibilitychange', aoMudarVisibilidade);

    return () => {
      parar();
      window.removeEventListener('mousemove', aoMoverMouse);
      window.removeEventListener('resize', aoRedimensionar);
      document.removeEventListener('mouseleave', aoSairMouse);
      document.removeEventListener('visibilitychange', aoMudarVisibilidade);
    };
  }, []);

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />;
}
