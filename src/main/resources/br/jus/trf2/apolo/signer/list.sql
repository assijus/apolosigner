select m.coddoc,
       m.codusuincl || to_char(m.dthrincl, 'ddmmyyyyhh24missFF9') as secret,

  (select formata_proc(p.numproccompl)
   from t_processo p
   where p.coddoc=m.coddoc) as processo,

  (select coalesce(
                     (select va.nomesint
                      from vara va
                      where m.codsecao=va.codsecao
                        and m.codlocfisvirt=va.codvara),
                     (select va.nome
                      from localfisico va
                      where m.codsecao=va.codsecao
                        and m.codlocfisvirt=va.codlocfis)) || ' - ' || c.descr
   from complemento c
   where c.codcompl=m.codcompl1) as ato,
       m.descrmotiv as motivo,
       m.dthrmov as data_hora_movimento,
       m.codsecao,

  (select u.codusu
   from usuario u
   where u.codsecao = m.codsecao
     and u.numcpf = ?) as codusu
from movimento m
where m.codsecao = nval_const('$$SecaoAtual')
  and m.coddoc in
    (--Busca todos os documentos da mesa de trabalho padrao
 select lvd.coddoc
     from localvirtualdocumento lvd,

       (--Busca o local virtual (mesa de trabalho) padrao do usuario para a sua lotacao principal
 select lvu.*
        from localvirtualusuario lvu,
             usuario u,
             usuariolotacao ul
        where u.numcpf = ?
          and lvu.codusu = u.codusu
          and u.codsecao = lvu.codsecao
          and lvu.codtiplocalvirt = nval_const('$$TipLocalVirtPadrao')
          and lvu.codsecao = nval_const('$$SecaoAtual')
          and ul.codsecao = lvu.codsecao
          and ul.codusu = u.codusu
          and lvu.codlocfis = ul.codlocfis -- AND ul.CodTipLot = nval_const('$$TipLotPrincUsu')
 ) mesa
     where lvd.codsecao = m.codsecao
       and lvd.codsecao = mesa.codsecao
       and lvd.codlocfis = mesa.codlocfis
       and lvd.codlocalvirt = mesa.codlocalvirt)--Verifica as fases que se deve assinar
and m.dthrmov =
    (select max(m1.dthrmov)
     from movimento m1
     where m1.codsecao = m.codsecao
       and m1.coddoc = m.coddoc
       and (m1.codfase in (nval_const('$$FaseResAud'),
                           nval_const('$$FaseDecis'),
                           nval_const('$$FaseRelatAcord'),
                           nval_const('$$FaseTransJulg'),
                           nval_const('$$FaseInfSecr'),
                           nval_const('$$FaseCert'),
                           nval_const('$$FaseRemIntConcl'),
                           nval_const('$$FaseRemInt'))
            or (m1.codfase in (nval_const('$$FaseConcl'),
                               nval_const('$$FaseInfSecr'))
                and exists
                  (select 1
                   from movimentociclo
                   where codsecao = m1.codsecao
                     and coddoc = m1.coddoc
                     and codfaseabert = m1.codfase
                     and dthrmovabert = m1.dthrmov
                     and dthrmovencer is null)))
       and not exists
         (select 1
          from faseconfiguracao fc
          where fc.codsecao = m1.codsecao
            and fc.codfase = m1.codfase
            and fc.codcfg = nval_const('$$CfgFaseAssinRestrita')
            and fc.codqualidoc = nval_const('$$QualiDocProc')
            and fc.valor = 'S'
            and not exists
              (select 1
               from usuario
               where codsecao = fc.codsecao
                 and codusu = usuariologado(m.codsecao,
                                              (select u.codusu
                                               from usuario u
                                               where u.codsecao = m.codsecao
                                                 and u.numcpf = ?)))) ) --Só busca os documentos que tem texto
and nvl(
          (select 'S'
           from movimentotexto mt
           where mt.codsecao = m.codsecao
             and mt.coddoc = m.coddoc
             and mt.dthrmov = m.dthrmov
             and mt.txtword is not null), 'N') = 'S' -- Somente se ninguém assinou o movimento
and not exists
    (select 1
     from documentoarquivo da
     where da.codsecao = m.codsecao
       and da.coddoc = m.coddoc
       and da.dthrmov = m.dthrmov
       and da.numtipmovarq=0 )