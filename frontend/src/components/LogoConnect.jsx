import { useId } from 'react';

// Símbolo do Connect CRM: três pontos ligados em triângulo, como uma rede de contatos.
// SVG puro, então fica nítido em qualquer tamanho. "tamanho" é a largura em px; a altura acompanha a proporção.
export default function LogoConnect({ tamanho = 56, className }) {
  // ids únicos, para não colidir se o logo aparecer mais de uma vez na página
  const idGradiente = useId();
  const idBrilho = useId();

  return (
    <svg
      className={className}
      width={tamanho}
      height={(tamanho * 52) / 80}
      viewBox="0 0 80 52"
      fill="none"
      role="img"
      aria-label="Connect CRM"
    >
      <defs>
        <linearGradient id={idGradiente} x1="10" y1="10" x2="70" y2="42" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#3b82f6" />
          <stop offset="1" stopColor="#38bdf8" />
        </linearGradient>
        {/* Brilho azul em volta das linhas e dos pontos */}
        <filter id={idBrilho} x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="1.6" result="desfoque" />
          <feMerge>
            <feMergeNode in="desfoque" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <g filter={`url(#${idBrilho})`} stroke={`url(#${idGradiente})`} strokeWidth="2.2">
        {/* Ligações entre os pontos */}
        <path d="M33 10 L10 40 L70 40 Z" strokeLinejoin="round" />

        {/* Pontos: o preenchimento escuro esconde a linha por dentro do círculo */}
        <circle cx="33" cy="10" r="6.5" fill="#0b1020" />
        <circle cx="10" cy="40" r="6.5" fill="#0b1020" />
        <circle cx="70" cy="40" r="6.5" fill="#0b1020" />
      </g>
    </svg>
  );
}
