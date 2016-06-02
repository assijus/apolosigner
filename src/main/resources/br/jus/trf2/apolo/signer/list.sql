select    m.coddoc, (select formata_proc(p.numproccompl) from t_processo p where p.coddoc=m.coddoc) as Processo, (select c.descr from complemento c where c.codcompl=m.codcompl1) as Ato, m.descrmotiv as Motivo, 
m.dthrmov as DATA_HORA_MOVIMENTO, m.CodSecao, (select u.CodUsu from Usuario u where u.CodSecao = m.CodSecao and u.NumCpf = ?) as CodUsu
from    Movimento m
where    m.CodSecao = 2
and    m.CodDoc in (    --Busca todos os documentos da mesa de trabalho padrão
            select    lvd.CodDoc
            from     LocalVirtualDocumento lvd, (    --Busca o local virtual (mesa de trabalho) padrão do usuário para a sua lotação principal
                                select    *
                                from     LocalVirtualUsuario  lvu
                                where    lvu.CodSecao = 2
                                and    lvu.CodUsu = (    --Busca o usuário pelo CPF   
                                            select    u.CodUsu
                                            from     Usuario u
                                            where    u.CodSecao = lvu.CodSecao
                                            --Caso o usuário não possua CPF, basta inverter as linhas abaixo.
                                            --and    u.Login = 'MPSCRR'
                                            and    u.NumCpf = ?)
                                and    lvu.CodLocFis = (--Busca a lotação principal do usuário
                                             select    ul.CodLocFis
                                              from     UsuarioLotacao ul
                                               where    ul.CodSecao = lvu.CodSecao    
                                             and    ul.CodUsu = (    --Busca o usuário pelo CPF
                                                         select    u.CodUsu
                                                         from     Usuario u
                                                        where     u.CodSecao = ul.CodSecao
                                                        --Caso o usuário não possua CPF, basta inverter as linhas abaixo.
                                                        --and    u.Login = 'MPSCRR'
                                                        and    u.NumCpf = ?)
                                              and     ul.CodTipLot = nval_const('$$TipLotPrincUsu'))
                                and    lvu.CodTipLocalVirt = nval_const('$$TipLocalVirtPadrao')) mesa
            where    lvd.CodSecao = m.CodSecao
            and    lvd.CodSecao = mesa.CodSecao
            and    lvd.CodLocFis = mesa.CodLocFis
            and    lvd.CodLocalVirt = mesa.CodLocalVirt)
--Verifica as fases que se deve assinar
and    m.DtHrMov = (    select    max( m1.DtHrMov )
            from    Movimento m1
            where    m1.CodSecao = m.CodSecao
            and    m1.CodDoc = m.CodDoc
            and    (m1.CodFase in (nval_const('$$FaseResAud'), nval_const('$$FaseDecis'), nval_const('$$FaseRelatAcord'))
                 or    (m1.CodFase in ( nval_const('$$FaseConcl'),nval_const('$$FaseInfSecr'))
                     and exists (select 1 from movimentociclo
                            where CodSecao = m1.CodSecao
                            and CodDoc = m1.CodDoc
                            and codfaseabert = m1.CodFase
                            and DtHrMovAbert = m1.DtHrMov
                            and DtHrMovEncer is null) ) )
          and     not exists (    select     1 from FaseConfiguracao fc
                        where      fc.CodSecao = m1.CodSecao
                        and     fc.CodFase = m1.CodFase
                        and      fc.CodCfg = nval_const('$$CfgFaseAssinRestrita')
                        and      fc.CodQualiDoc = nval_const( '$$QualiDocProc' )
                        and     fc.Valor = 'S'
                        and     not  exists (select    1
                                from       Usuario
                                where      CodSecao = fc.CodSecao
                                and        CodUsu = USUARIOLOGADO(m.codsecao,(select    u.CODUSU
                                                         from     Usuario u
                                                        where     u.CodSecao = M.CodSecao
                                                        --Caso o usuário não possua CPF, basta inverter as linhas abaixo.
                                                        --and    u.Login = 'MPSCRR'
                                                        and    u.NumCpf = ?))
                                and        CodFunc in (nval_const('$$FuncJuiz'), nval_const('$$FuncJuiza'),
                                        nval_const('$$FuncJuizSubst'), nval_const('$$FuncJuizaSubst'))))
            )
--Só busca os documentos que tem texto
and    nvl( (    select    'S'
        from    MovimentoTexto mt
        where    mt.CodSecao = m.CodSecao
        and    mt.CodDoc = m.CodDoc
        and    mt.DtHrMov = m.DtHrMov
        and    mt.TxtWord is not null ), 'N' ) = 'S'
-- Somente se ninguém assinou o movimento
and    not Exists (    select    1
            from    DocumentoArquivo da
            where    da.CodSecao = m.CodSecao
            and    da.CodDoc = m.CodDoc
            and    da.DtHrMov = m.DtHrMov
            AND da.Numtipmovarq=0
           )
