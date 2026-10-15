"""Copy the public schema to an EMPTY PostgreSQL database; never deletes source/target tables.
Use native PostgreSQL 16+ tools and SOURCE_DATABASE_URL / TARGET_DATABASE_URL in the environment.
Pause application writes and set MIGRATION_WRITES_PAUSED=true before running.
"""
import datetime, json, os, pathlib, shutil, subprocess, sys
from urllib.parse import urlparse, parse_qs, unquote

def connection(name):
    value = os.environ.get(name, '')
    url = urlparse(value)
    if url.scheme not in ('postgres', 'postgresql') or not url.hostname or not url.username or not url.path.strip('/'):
        raise RuntimeError('Configure ' + name + ' com uma URL PostgreSQL completa, somente no ambiente.')
    options = parse_qs(url.query)
    local = url.hostname in ('localhost', '127.0.0.1')
    sslmode = options.get('sslmode', ['prefer' if local else 'require'])[0]
    if not local and sslmode not in ('require', 'verify-ca', 'verify-full'):
        raise RuntimeError('TLS é obrigatório para bancos remotos.')
    if '-pooler' in url.hostname:
        raise RuntimeError('Use a conexão direta, sem pooler, para migração.')
    env = os.environ.copy()
    env.update(PGHOST=url.hostname, PGPORT=str(url.port or 5432), PGDATABASE=unquote(url.path[1:]),
               PGUSER=unquote(url.username), PGPASSWORD=unquote(url.password or ''), PGSSLMODE=sslmode,
               PGCONNECT_TIMEOUT='15')
    return env

def run(args, env, capture=True):
    result = subprocess.run(args, env=env, text=True, capture_output=capture)
    if result.returncode:
        # Do not echo connection strings, credentials or provider error payloads.
        raise RuntimeError(args[0] + ' falhou; confira conectividade, permissões e versão das ferramentas. O banco de origem não foi alterado.')
    return result.stdout.strip() if capture else ''

def sql(env, query):
    return run(['psql', '-X', '-A', '-t', '-v', 'ON_ERROR_STOP=1', '-c', query], env)

def counts(env):
    tables = json.loads(sql(env, "select coalesce(json_agg(tablename order by tablename),'[]'::json) from pg_tables where schemaname='public'"))
    result = {}
    for table in tables:
        identifier = '"' + table.replace('"', '""') + '"'
        result[table] = int(sql(env, 'select count(*) from public.' + identifier))
    return result

def main():
    if os.environ.get('MIGRATION_WRITES_PAUSED') != 'true':
        raise RuntimeError('Pause as escritas da API e do worker, então defina MIGRATION_WRITES_PAUSED=true.')
    for tool in ('psql', 'pg_dump', 'pg_restore'):
        if not shutil.which(tool):
            raise RuntimeError('Instale as ferramentas oficiais PostgreSQL 16+ e disponibilize ' + tool + ' no PATH.')
    source, target = connection('SOURCE_DATABASE_URL'), connection('TARGET_DATABASE_URL')
    identity = lambda env: (env['PGHOST'].lower(), env['PGPORT'], env['PGDATABASE'])
    if identity(source) == identity(target):
        raise RuntimeError('Origem e destino devem ser bancos diferentes.')
    before = counts(source)
    if not {'channels', 'notifications', 'delivery_attempts', 'flyway_schema_history'}.issubset(before):
        raise RuntimeError('A origem não contém o esquema esperado da Notification API.')
    if counts(target):
        raise RuntimeError('Destino não está vazio. A migração não apaga nem sobrescreve tabelas.')
    folder = pathlib.Path('backups')
    folder.mkdir(exist_ok=True)
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    archive = folder / ('notification-' + stamp + '.dump')
    if archive.exists():
        raise RuntimeError('Backup com este nome já existe; aguarde um segundo.')
    run(['pg_dump', '--format=custom', '--schema=public', '--no-owner', '--no-acl', '--no-comments', '--file=' + str(archive)], source)
    os.chmod(archive, 0o600)
    listing = run(['pg_restore', '--list', str(archive)], source)
    # New PostgreSQL databases already contain public; retain every table, sequence and constraint.
    filtered = '\n'.join(line for line in listing.splitlines() if ' SCHEMA - public ' not in line)
    toc = archive.with_suffix('.list')
    toc.write_text(filtered, encoding='utf-8')
    run(['pg_restore', '--dbname=' + target['PGDATABASE'], '--single-transaction', '--exit-on-error',
         '--no-owner', '--no-acl', '--use-list=' + str(toc), str(archive)], target)
    after = counts(target)
    if before != after or counts(source) != before:
        raise RuntimeError('As contagens divergiram ou a origem recebeu novas escritas. Não altere a configuração da API. Preserve o backup e revise a migração.')
    report = archive.with_suffix('.verification.json')
    report.write_text(json.dumps({'verified_at': stamp, 'row_counts': after}, indent=2), encoding='utf-8')
    print('Cópia verificada. Backup preservado em ' + str(archive) + '. A API ainda não foi reconfigurada.')

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Migração interrompida: ' + str(error), file=sys.stderr)
        sys.exit(1)
