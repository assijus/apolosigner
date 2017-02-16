SELECT m.coddoc, m.codusuincl || To_char(m.dthrincl, 'ddmmyyyyhh24missFF9') as secret,

  (SELECT formata_proc(p.numproccompl)
   FROM t_processo p
   WHERE p.coddoc=m.coddoc) AS Processo,

  (SELECT c.descr
   FROM complemento c
   WHERE c.codcompl=m.codcompl1) AS Ato,
       m.descrmotiv AS Motivo,
       m.dthrmov AS DATA_HORA_MOVIMENTO,
       m.CodSecao,

  (SELECT u.CodUsu
   FROM Usuario u
   WHERE u.CodSecao = m.CodSecao
     AND u.NumCpf = ?) AS CodUsu
FROM Movimento m
WHERE m.CodSecao = Nval_const('$$SecaoAtual')
  AND m.CodDoc IN (--Busca todos os documentos da mesa de trabalho padrao

                   SELECT lvd.CodDoc
                   FROM LocalVirtualDocumento lvd,
                        (--Busca o local virtual (mesa de trabalho) padrao do usuario para a sua lotacao principal

                         SELECT *
                         FROM LocalVirtualUsuario lvu
                         WHERE lvu.CodSecao = Nval_const('$$SecaoAtual')
                           AND lvu.CodUsu = (--Busca o usuario pelo CPF

                                             SELECT u.CodUsu
                                             FROM Usuario u
                                             WHERE u.CodSecao = lvu.CodSecao
                                               AND u.NumCpf = ?)
                           AND lvu.CodLocFis = (--Busca a lotacao principal do usuario

                                                SELECT ul.CodLocFis
                                                FROM UsuarioLotacao ul
                                                WHERE ul.CodSecao = lvu.CodSecao
                                                  AND ul.CodUsu = (--Busca o usuario pelo CPF

                                                                   SELECT u.CodUsu
                                                                   FROM Usuario u
                                                                   WHERE u.CodSecao = ul.CodSecao
                                                                     AND u.NumCpf = ?)
                                                  AND ul.CodTipLot = nval_const('$$TipLotPrincUsu'))
                           AND lvu.CodTipLocalVirt = nval_const('$$TipLocalVirtPadrao')) mesa
                   WHERE lvd.CodSecao = m.CodSecao
                     AND lvd.CodSecao = mesa.CodSecao
                     AND lvd.CodLocFis = mesa.CodLocFis
                     AND lvd.CodLocalVirt = mesa.CodLocalVirt)--Verifica as fases que se deve assinar
AND m.DtHrMov =
    (SELECT max(m1.DtHrMov)
     FROM Movimento m1
     WHERE m1.CodSecao = m.CodSecao
       AND m1.CodDoc = m.CodDoc
       AND (m1.CodFase IN (nval_const('$$FaseResAud'),
                           nval_const('$$FaseDecis'),
                           nval_const('$$FaseRelatAcord'),
                           nval_const('$$FaseTransJulg'),
                           nval_const('$$FaseInfSecr'),
                           nval_const('$$FaseCert'),
                           nval_const('$$FaseRemIntConcl'),
                           nval_const('$$FaseRemInt'))
            OR (m1.CodFase IN (nval_const('$$FaseConcl'),
                               nval_const('$$FaseInfSecr'))
                AND EXISTS
                  (SELECT 1
                   FROM movimentociclo
                   WHERE CodSecao = m1.CodSecao
                     AND CodDoc = m1.CodDoc
                     AND codfaseabert = m1.CodFase
                     AND DtHrMovAbert = m1.DtHrMov
                     AND DtHrMovEncer IS NULL)))
       AND NOT EXISTS
         (SELECT 1
          FROM FaseConfiguracao fc
          WHERE fc.CodSecao = m1.CodSecao
            AND fc.CodFase = m1.CodFase
            AND fc.CodCfg = nval_const('$$CfgFaseAssinRestrita')
            AND fc.CodQualiDoc = nval_const('$$QualiDocProc')
            AND fc.Valor = 'S'
            AND NOT EXISTS
              (SELECT 1
               FROM Usuario
               WHERE CodSecao = fc.CodSecao
                 AND CodUsu = USUARIOLOGADO(m.codsecao,
                                              (SELECT u.CODUSU
                                               FROM Usuario u
                                               WHERE u.CodSecao = M.CodSecao
                                                 AND u.NumCpf = ?)))) ) --Só busca os documentos que tem texto
AND nvl(
          (SELECT 'S'
           FROM MovimentoTexto mt
           WHERE mt.CodSecao = m.CodSecao
             AND mt.CodDoc = m.CodDoc
             AND mt.DtHrMov = m.DtHrMov
             AND mt.TxtWord IS NOT NULL), 'N') = 'S' -- Somente se ninguém assinou o movimento
AND NOT EXISTS
    (SELECT 1
     FROM DocumentoArquivo da
     WHERE da.CodSecao = m.CodSecao
       AND da.CodDoc = m.CodDoc
       AND da.DtHrMov = m.DtHrMov
       AND da.Numtipmovarq=0 )