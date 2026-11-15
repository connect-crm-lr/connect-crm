// Formatação só visual: telefone e CPF/CNPJ são gravados apenas com dígitos (regra 6.4).
export const soDigitos = (valor) => String(valor ?? '').replace(/\D/g, '');

// Máscara enquanto digita, sem o DDI: (62) 99999-8888 ou (62) 3333-4444.
export function mascararTelefone(valor) {
  let d = soDigitos(valor);
  // Número colado com DDI (ex.: 5562999998888): tira o 55 antes de cortar. Com 11 dígitos ou menos
  // não mexe, porque 55 também é DDD (RS) e o DDI é acrescentado só no envio (telefoneParaApi).
  if (d.length >= 12 && d.startsWith('55')) d = d.slice(2);
  d = d.slice(0, 11);
  if (d.length <= 2) return d ? `(${d}` : '';
  const ddd = d.slice(0, 2);
  const resto = d.slice(2);
  const corte = resto.length > 8 ? 5 : 4;
  return resto.length <= corte
    ? `(${ddd}) ${resto}`
    : `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}`;
}

// Telefone gravado (55 + DDD + número) para exibição.
export function formatarTelefone(telefone) {
  const d = soDigitos(telefone);
  return mascararTelefone(d.startsWith('55') ? d.slice(2) : d);
}

// Telefone digitado (DDD + número) -> valor da API, com o DDI 55.
export const telefoneParaApi = (mascarado) => `55${soDigitos(mascarado)}`;

// Telefone da API -> campo do formulário (sem o DDI).
export const telefoneParaCampo = (telefone) => {
  const d = soDigitos(telefone);
  return mascararTelefone(d.startsWith('55') ? d.slice(2) : d);
};

// CPF (11 dígitos) ou CNPJ (14) com máscara; devolve os dígitos como estão se não for nenhum dos dois.
export function formatarDocumento(valor) {
  const d = soDigitos(valor);
  if (d.length === 11) return d.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  if (d.length === 14) return d.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
  return d;
}

// Dinheiro: R$ 34,00. Número do pedido: #P-0000. Metragem: 12,5 m. Datas: dd/mm/aaaa.
export const formatarDinheiro = (valor) =>
  Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const formatarNumeroPedido = (id) => `#P-${String(id).padStart(4, '0')}`;

export const formatarMetragem = (valor) =>
  `${Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} m`;

// A API manda 'AAAA-MM-DD' ou 'AAAA-MM-DDTHH:mm:ss'; a tela mostra dd/mm/aaaa, sem Date (o fuso não muda o dia).
export function formatarData(dataIso) {
  if (!dataIso) return '';
  const [ano, mes, dia] = String(dataIso).slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}
