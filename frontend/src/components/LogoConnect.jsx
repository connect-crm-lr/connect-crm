import { useId } from 'react';

// Símbolo do Connect CRM: três pontos ligados, como uma rede de contatos.
// SVG puro, então fica nítido em qualquer tamanho. "tamanho" é a largura em px.
export default function LogoConnect({ tamanho = 32, className }) {
  // id único do gradiente, para não colidir se o logo aparecer mais de uma vez na página
  const idGradiente = useId();

  return (
    <svg
      className={className}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 40 40"
      fill="none"
      role="img"
      aria-label="Connect CRM"
    >
      <defs>
        <linearGradient id={idGradiente} x1="6" y1="6" x2="34" y2="34" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#5b7cff" />
          <stop offset="1" stopColor="#38bdf8" />
        </linearGradient>
      </defs>

      {/* Ligações entre os pontos */}
      <path d="M22 9 L9 29 L33 29 Z" stroke={`url(#${idGradiente})`} strokeWidth="2" strokeLinejoin="round" />

      {/* Pontos: o preenchimento escuro esconde a linha por dentro do círculo */}
      <circle cx="22" cy="9" r="4.5" fill="#0b1020" stroke={`url(#${idGradiente})`} strokeWidth="2" />
      <circle cx="9" cy="29" r="4.5" fill="#0b1020" stroke={`url(#${idGradiente})`} strokeWidth="2" />
      <circle cx="33" cy="29" r="4.5" fill="#0b1020" stroke={`url(#${idGradiente})`} strokeWidth="2" />
    </svg>
  );
}
