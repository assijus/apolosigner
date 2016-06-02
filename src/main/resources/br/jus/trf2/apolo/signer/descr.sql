select 
	(select f.Descr from Fase f where f.CodSecao = m.CodSecao and f.CodFase = m.CodFase) || ' - ' || (select c.Descr from Complemento c where c.CodSecao = m.CodSecao and c.CodCompl = m.CodCompl1) as descr
from     
	Movimento m
where     
	m.CodSecao = ?
	and m.CodDoc = ?
	and m.DtHrMov = ?
