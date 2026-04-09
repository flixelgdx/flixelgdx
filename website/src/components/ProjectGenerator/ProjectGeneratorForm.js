import useBaseUrl from '@docusaurus/useBaseUrl';
import Heading from '@theme/Heading';
import {useCallback, useEffect, useState} from 'react';

import {buildProjectZip} from './buildProjectZip';

import styles from './styles.module.css';

const FALLBACK_RELEASE_TAGS = ['0.1.1-beta', '0.1.0-beta'];

const RELEASES_API =
  'https://api.github.com/repos/stringdotjar/flixelgdx/releases?per_page=20';

export function ProjectGeneratorForm() {
  const [desktop, setDesktop] = useState(true);
  const [android, setAndroid] = useState(false);
  const [ios, setIos] = useState(false);
  const [web, setWeb] = useState(false);
  const [projectName, setProjectName] = useState('mygame');
  const [packageName, setPackageName] = useState('com.example.mygame');
  const [editor, setEditor] = useState('intellij');
  const [construto, setConstruto] = useState(false);
  const [releaseTags, setReleaseTags] = useState(FALLBACK_RELEASE_TAGS);
  const [versionMode, setVersionMode] = useState('release');
  const [selectedTag, setSelectedTag] = useState(FALLBACK_RELEASE_TAGS[0]);
  const [customVersion, setCustomVersion] = useState('');
  const [jvmArgs, setJvmArgs] = useState(
    '-Xms256M -Xmx1G -Dfile.encoding=UTF-8',
  );
  const [extraGradleProps, setExtraGradleProps] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const r = await fetch(RELEASES_API);
        if (!r.ok) {
          return;
        }
        const data = await r.json();
        if (!Array.isArray(data)) {
          return;
        }
        const tags = data
          .map((x) => x.tag_name)
          .filter((t) => typeof t === 'string' && t.length > 0);
        if (!cancelled && tags.length > 0) {
          setReleaseTags(tags);
          setSelectedTag(tags[0]);
        }
      } catch {
        /* keep fallback list */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const siteBase = useBaseUrl('/');

  const fetchAsset = useCallback(
    (relPath) => {
      const path = relPath.replace(/^\/+/, '');
      const base = siteBase.endsWith('/') ? siteBase : `${siteBase}/`;
      return fetch(`${base}${path}`);
    },
    [siteBase],
  );

  const effectiveFlixelVersion =
    versionMode === 'custom'
      ? (customVersion || '').trim() || FALLBACK_RELEASE_TAGS[0]
      : selectedTag;

  const onDownload = useCallback(async () => {
    setError(null);
    setBusy(true);
    try {
      const logoRes = await fetchAsset('img/flixelgdx-logo.png');
      if (!logoRes.ok) {
        throw new Error(`Could not load logo asset (${logoRes.status})`);
      }
      const logoBytes = new Uint8Array(await logoRes.arrayBuffer());
      await buildProjectZip({
        fetchAsset,
        logoBytes,
        projectNameRaw: projectName,
        package: packageName,
        flixelVersion: effectiveFlixelVersion,
        desktop,
        android,
        ios,
        web,
        editor,
        construto,
        extraGradleProps,
        jvmArgs,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }, [
    android,
    construto,
    customVersion,
    desktop,
    editor,
    effectiveFlixelVersion,
    extraGradleProps,
    fetchAsset,
    ios,
    jvmArgs,
    packageName,
    projectName,
    selectedTag,
    versionMode,
    web,
  ]);

  return (
    <>
      <p className={styles.lead}>
        Pick your platforms and options, then download a ready-to-open Gradle
        project. The archive includes <code>README.txt</code> with setup steps
        for your editor.
      </p>

      <section className={styles.card}>
        <Heading as="h2" className={styles.h2}>
          Platforms
        </Heading>
        <ul className={styles.checkList}>
          <li>
            <label>
              <input
                type="checkbox"
                checked={desktop}
                onChange={(e) => setDesktop(e.target.checked)}
              />
              Desktop (LWJGL3)
            </label>
          </li>
          <li>
            <label>
              <input
                type="checkbox"
                checked={android}
                onChange={(e) => setAndroid(e.target.checked)}
              />
              Android{' '}
              <span className={styles.warn}>(not fully supported yet)</span>
            </label>
          </li>
          <li>
            <label>
              <input
                type="checkbox"
                checked={ios}
                onChange={(e) => setIos(e.target.checked)}
              />
              iOS{' '}
              <span className={styles.warn}>(not fully supported yet)</span>
            </label>
          </li>
          <li>
            <label>
              <input
                type="checkbox"
                checked={web}
                onChange={(e) => setWeb(e.target.checked)}
              />
              Web (TeaVM)
            </label>
          </li>
        </ul>
      </section>

      <section className={styles.card}>
        <Heading as="h2" className={styles.h2}>
          Project
        </Heading>
        <div className={styles.field}>
          <label htmlFor="pg-name">Project name (Gradle root name)</label>
          <input
            id="pg-name"
            className={styles.input}
            value={projectName}
            onChange={(e) => setProjectName(e.target.value)}
            autoComplete="off"
          />
        </div>
        <div className={styles.field}>
          <label htmlFor="pg-pkg">Java package (e.g. com.mycompany.game)</label>
          <input
            id="pg-pkg"
            className={styles.input}
            value={packageName}
            onChange={(e) => setPackageName(e.target.value)}
            autoComplete="off"
          />
        </div>
        <div className={styles.field}>
          <label htmlFor="pg-flixel-mode">FlixelGDX version</label>
          <select
            id="pg-flixel-mode"
            className={styles.input}
            value={versionMode}
            onChange={(e) => setVersionMode(e.target.value)}>
            <option value="release">Published GitHub release (recommended)</option>
            <option value="custom">Custom JitPack ref</option>
          </select>
          {versionMode === 'release' ? (
            <select
              id="pg-flixel-tag"
              className={styles.input}
              style={{marginTop: '0.5rem'}}
              value={selectedTag}
              onChange={(e) => setSelectedTag(e.target.value)}>
              {releaseTags.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          ) : (
            <input
              id="pg-flixel-custom"
              className={styles.input}
              style={{marginTop: '0.5rem'}}
              value={customVersion}
              onChange={(e) => setCustomVersion(e.target.value)}
              placeholder="e.g. main-SNAPSHOT or commit hash"
            />
          )}
          <p className={styles.hint}>
            JitPack uses{' '}
            <code>com.github.stringdotjar.flixelgdx:&lt;module&gt;:&lt;tag&gt;</code>
            . Release tags are loaded from GitHub when possible; otherwise the
            list falls back to known betas.
          </p>
        </div>
      </section>

      <section className={styles.card}>
        <Heading as="h2" className={styles.h2}>
          Editor & tooling
        </Heading>
        <div className={styles.field}>
          <label htmlFor="pg-editor">Primary editor</label>
          <select
            id="pg-editor"
            className={styles.input}
            value={editor}
            onChange={(e) => setEditor(e.target.value)}>
            <option value="intellij">IntelliJ IDEA</option>
            <option value="eclipse">Eclipse</option>
            <option value="vscode">VS Code</option>
            <option value="other">Other / CLI only</option>
          </select>
        </div>
        <label className={styles.inline}>
          <input
            type="checkbox"
            checked={construto && desktop}
            disabled={!desktop}
            onChange={(e) => setConstruto(e.target.checked)}
          />
          Add Construo to desktop (native Linux / Windows / macOS packages; requires desktop
          module)
        </label>
      </section>

      <section className={styles.card}>
        <Heading as="h2" className={styles.h2}>
          Gradle
        </Heading>
        <div className={styles.field}>
          <label htmlFor="pg-jvmargs">org.gradle.jvmargs</label>
          <input
            id="pg-jvmargs"
            className={styles.input}
            value={jvmArgs}
            onChange={(e) => setJvmArgs(e.target.value)}
          />
        </div>
        <div className={styles.field}>
          <label htmlFor="pg-extra">Extra gradle.properties lines</label>
          <textarea
            id="pg-extra"
            className={styles.textarea}
            value={extraGradleProps}
            onChange={(e) => setExtraGradleProps(e.target.value)}
            rows={4}
            placeholder="# android.useAndroidX=true"
          />
        </div>
      </section>

      {error ? <p className={styles.error}>{error}</p> : null}

      <div className={styles.actions}>
        <button
          type="button"
          className="button button--primary button--lg"
          disabled={busy}
          onClick={() => void onDownload()}>
          {busy ? 'Building zip…' : 'Download project zip'}
        </button>
      </div>
    </>
  );
}
