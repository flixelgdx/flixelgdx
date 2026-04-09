import Head from '@docusaurus/Head';
import useBaseUrl from '@docusaurus/useBaseUrl';
import Layout from '@theme/Layout';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';

import styles from './api.module.css';

const DOKKA_PREFIX = '/dokka-html';
const GITHUB_TREE =
  'https://github.com/stringdotjar/flixelgdx/tree/master';

const MODULE_ORDER = [
  {id: 'flixelgdx-core', label: 'Core'},
  {id: 'flixelgdx-lwjgl3', label: 'Desktop (LWJGL3)'},
  {id: 'flixelgdx-android', label: 'Android'},
  {id: 'flixelgdx-ios', label: 'iOS'},
  {id: 'flixelgdx-teavm', label: 'HTML (TeaVM)'},
  {id: 'flixelgdx-teavm-plugin', label: 'TeaVM Plugin'},
  {id: 'flixelgdx-jvm', label: 'JVM'},
];

const MODULE_LABELS = Object.fromEntries(
  MODULE_ORDER.map((m) => [m.id, m.label]),
);

const KOTLIN_JAVA_TYPES = {
  Int: 'int',
  Long: 'long',
  Float: 'float',
  Double: 'double',
  Boolean: 'boolean',
  Byte: 'byte',
  Short: 'short',
  Char: 'char',
  Unit: 'void',
  Any: 'Object',
  Nothing: 'void',
  String: 'String',
};

const JAVA_KEYWORDS = new Set([
  'abstract','class','interface','enum','extends','implements',
  'static','final','void','int','long','float','double','boolean',
  'byte','short','char','public','protected','private','default',
  'new','return','this','super','null','true','false','throws',
]);

function kotlinTypeToJava(text) {
  for (const [k, j] of Object.entries(KOTLIN_JAVA_TYPES)) {
    if (k === 'String') continue;
    text = text.replace(new RegExp(`\\b${k}\\b`, 'g'), j);
  }
  return text;
}

function colorizeSignature(sig) {
  return sig.split(/(\b\w+\b|[^\w]+)/g).map((token, i) => {
    if (JAVA_KEYWORDS.has(token)) {
      return <span key={i} className={styles.kwRed}>{token}</span>;
    }
    if (/^[A-Z]/.test(token) && token.length > 1) {
      return <span key={i} className={styles.kwType}>{token}</span>;
    }
    return token;
  });
}

function colorizeCodeBlock(code) {
  return code.replace(
    /\b(class|interface|enum|extends|implements|static|final|void|int|long|float|double|boolean|byte|short|char|public|protected|private|abstract|new|return|this|super|null|true|false|throws|if|else|for|while|do|switch|case|break|continue|try|catch|finally|import|package)\b/g,
    '<span class="' + styles.kwRed + '">$1</span>',
  ).replace(
    /\b([A-Z]\w{1,})\b/g,
    (m, name) => JAVA_KEYWORDS.has(name) ? m : '<span class="' + styles.kwType + '">' + name + '</span>',
  ).replace(
    /(\/\/[^\n]*)/g,
    '<span class="' + styles.kwComment + '">$1</span>',
  ).replace(
    /("(?:[^"\\]|\\.)*")/g,
    '<span class="' + styles.kwString + '">$1</span>',
  );
}

function transformSignature(rawHtml, className) {
  let sig = rawHtml
    .replace(/<wbr\s*\/?>/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&\w+;/g, ' ')
    .replace(/\u00A0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  sig = sig.replace(/@[\w.:]+(?:\([^)]*\))?\s*/g, '');
  sig = sig.replace(/\b(open|override|internal)\s+/g, '');

  let modifiers = [];

  if (/\bprotected\b/.test(sig)) {
    modifiers.push('protected');
    sig = sig.replace(/\bprotected\s+/g, '');
  }
  if (/\bprivate\b/.test(sig)) {
    modifiers.push('private');
    sig = sig.replace(/\bprivate\s+/g, '');
  }

  const isStatic = /\bstatic\b/.test(sig);
  if (isStatic) modifiers.push('static');

  if (sig.startsWith('constructor')) {
    const paramPart = sig.slice('constructor'.length);
    const converted = convertParams(paramPart);
    sig = `${className || 'Constructor'}${converted}`;
    return {text: kotlinTypeToJava(sig), modifiers, isStatic: false};
  }

  if (/^(abstract\s+)?fun\s+/.test(sig)) {
    if (/^abstract\s+/.test(sig)) {
      modifiers.push('abstract');
      sig = sig.replace(/^abstract\s+/, '');
    }
    const match = sig.match(/^fun\s+(\w+)\s*(\([^)]*\))\s*(?::\s*(.+))?$/);
    if (match) {
      const name = match[1];
      const params = convertParams(match[2]);
      const ret = match[3] ? kotlinTypeToJava(match[3].trim()) : 'void';
      sig = `${ret} ${name}${params}`;
    } else {
      sig = sig.replace(/^fun\s+/, 'void ');
      sig = convertKotlinParams(kotlinTypeToJava(sig));
    }
    return {text: sig, modifiers, isStatic};
  }

  if (/^(abstract\s+)?val\s+/.test(sig)) {
    if (/^abstract\s+/.test(sig)) {
      modifiers.push('abstract');
      sig = sig.replace(/^abstract\s+/, '');
    }
    modifiers.push('final');
    const match = sig.match(/^val\s+(\w+)\s*:\s*(.+)$/);
    if (match) {
      sig = `${kotlinTypeToJava(match[2].trim())} ${match[1]}`;
    } else {
      sig = sig.replace(/^val\s+/, '');
      sig = kotlinTypeToJava(sig);
    }
    return {text: sig, modifiers, isStatic};
  }

  if (/^(abstract\s+)?var\s+/.test(sig)) {
    if (/^abstract\s+/.test(sig)) {
      modifiers.push('abstract');
      sig = sig.replace(/^abstract\s+/, '');
    }
    const match = sig.match(/^var\s+(\w+)\s*:\s*(.+)$/);
    if (match) {
      sig = `${kotlinTypeToJava(match[2].trim())} ${match[1]}`;
    } else {
      sig = sig.replace(/^var\s+/, '');
      sig = kotlinTypeToJava(sig);
    }
    return {text: sig, modifiers, isStatic};
  }

  if (/^(abstract\s+)?(class|interface|enum)\s+/.test(sig)) {
    const isInterface = /\binterface\b/.test(sig);
    const colonIdx = sig.indexOf(':');
    if (colonIdx !== -1) {
      const before = sig.slice(0, colonIdx).trim();
      const after = sig.slice(colonIdx + 1).trim();
      const parents = after.split(',').map((s) => s.trim()).filter(Boolean);
      if (isInterface) {
        sig = `${before} extends ${parents.join(', ')}`;
      } else if (parents.length === 1) {
        sig = `${before} extends ${parents[0]}`;
      } else if (parents.length > 1) {
        sig = `${before} extends ${parents[0]} implements ${parents.slice(1).join(', ')}`;
      } else {
        sig = before;
      }
    }
    sig = kotlinTypeToJava(sig);
    return {text: sig, modifiers, isStatic};
  }

  const ctorMatch = sig.match(/^([A-Z]\w*)\s*(\([^)]*\))$/);
  if (ctorMatch) {
    const name = ctorMatch[1];
    const converted = convertParams(ctorMatch[2]);
    sig = `${name}${converted}`;
    return {text: kotlinTypeToJava(sig), modifiers, isStatic: false};
  }

  sig = convertKotlinParams(kotlinTypeToJava(sig));
  return {text: sig, modifiers, isStatic};
}

function buildSignatureText(text, modifiers) {
  const prefix = modifiers.length > 0 ? modifiers.join(' ') + ' ' : '';
  return prefix + text;
}

function convertKotlinParams(sig) {
  const parenIdx = sig.indexOf('(');
  if (parenIdx === -1) return sig;
  const closeIdx = sig.lastIndexOf(')');
  if (closeIdx === -1) return sig;
  const before = sig.slice(0, parenIdx);
  const inner = sig.slice(parenIdx + 1, closeIdx).trim();
  const after = sig.slice(closeIdx + 1);
  if (!inner) return sig;
  const params = splitTopLevelComma(inner).map((p) => {
    const m = p.trim().match(/^(\w+)\s*:\s*(.+)$/);
    if (m) return `${kotlinTypeToJava(m[2].trim())} ${m[1]}`;
    return kotlinTypeToJava(p.trim());
  });
  return `${before}(${params.join(', ')})${after}`;
}

function splitTopLevelComma(str) {
  const parts = [];
  let depth = 0;
  let start = 0;
  for (let i = 0; i < str.length; i++) {
    if (str[i] === '<') depth++;
    else if (str[i] === '>') depth--;
    else if (str[i] === ',' && depth === 0) {
      parts.push(str.slice(start, i));
      start = i + 1;
    }
  }
  parts.push(str.slice(start));
  return parts;
}

function convertParams(paramStr) {
  if (!paramStr) return '()';
  const inner = paramStr.replace(/^\(/, '').replace(/\)$/, '').trim();
  if (!inner) return '()';
  const params = splitTopLevelComma(inner).map((p) => {
    const m = p.trim().match(/^(\w+)\s*:\s*(.+)$/);
    if (m) return `${kotlinTypeToJava(m[2].trim())} ${m[1]}`;
    return kotlinTypeToJava(p.trim());
  });
  return `(${params.join(', ')})`;
}

function parseDokkaHtml(htmlString, className, dokkaBaseUrl) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlString, 'text/html');
  const content =
    doc.querySelector('#content') || doc.querySelector('.main-content');
  if (!content) return null;

  const pageType = content.getAttribute('data-page-type') || 'module';

  const titleEl = content.querySelector('h1.cover');
  const title = titleEl
    ? titleEl.textContent.replace(/\s+/g, ' ').trim()
    : '';

  const coverSymbol = content.querySelector(
    '.cover > .platform-hinted .symbol.monospace',
  );
  let declaration = '';
  if (coverSymbol) {
    const result = transformSignature(coverSymbol.innerHTML, className);
    declaration = buildSignatureText(result.text, result.modifiers);
  }

  let description = '';
  const coverHinted = content.querySelector(
    '.cover > .platform-hinted .content',
  );
  if (coverHinted) {
    description = parseDocContent(coverHinted, dokkaBaseUrl);
  }

  const sections = [];
  const togglables = content.querySelectorAll(
    '.tabbedcontent [data-togglable]',
  );
  for (const section of togglables) {
    const h2 = section.querySelector('h2');
    if (!h2) continue;
    const sectionTitle = h2.textContent.trim();
    const members = [];

    for (const row of section.querySelectorAll('.table-row')) {
      const nameEl = row.querySelector('.main-subrow a');
      const name = nameEl
        ? nameEl.textContent.replace(/\s+/g, '').trim()
        : '';
      const memberHref = nameEl ? nameEl.getAttribute('href') : null;

      const contentEls = row.querySelectorAll('.content');
      let foundMembers = false;
      let overloadIdx = 0;
      for (const contentEl of contentEls) {
        let currentSig = null;
        for (const child of contentEl.children) {
          const isSig =
            child.classList.contains('symbol') &&
            child.classList.contains('monospace');
          const isBrief = child.classList.contains('brief');

          if (isSig) {
            if (currentSig) {
              members.push({...currentSig});
              foundMembers = true;
              overloadIdx++;
            }
            const result = transformSignature(
              child.innerHTML,
              className || name,
            );
            currentSig = {
              name,
              signature: buildSignatureText(result.text, result.modifiers),
              isStatic: result.isStatic,
              brief: '',
              memberHref,
              overloadIndex: overloadIdx,
            };
          } else if (isBrief && currentSig) {
            currentSig.brief = cleanHtml(child.innerHTML, dokkaBaseUrl);
            members.push({...currentSig});
            foundMembers = true;
            currentSig = null;
            overloadIdx++;
          }
        }
        if (currentSig) {
          members.push({...currentSig});
          foundMembers = true;
          overloadIdx++;
        }
      }

      if (!foundMembers) {
        const sigEls = row.querySelectorAll('.symbol.monospace');
        let idx = 0;
        for (const sigEl of sigEls) {
          const result = transformSignature(
            sigEl.innerHTML,
            className || name,
          );
          const briefEl = row.querySelector('.brief');
          members.push({
            name,
            signature: buildSignatureText(result.text, result.modifiers),
            isStatic: result.isStatic,
            brief: briefEl ? cleanHtml(briefEl.innerHTML, dokkaBaseUrl) : '',
            memberHref,
            overloadIndex: idx++,
          });
        }
      }
    }

    if (members.length > 0) {
      sections.push({title: sectionTitle, members});
    }
  }

  if (sections.length === 0 && pageType !== 'classlike') {
    const packageRows = content.querySelectorAll('.table .table-row');
    const members = [];
    for (const row of packageRows) {
      const nameEl = row.querySelector('.main-subrow a');
      const name = nameEl ? nameEl.textContent.trim() : '';
      const briefEl = row.querySelector('.paragraph, .brief');
      const brief = briefEl ? cleanHtml(briefEl.innerHTML, dokkaBaseUrl) : '';
      if (name) {
        members.push({
          name,
          signature: '',
          isStatic: false,
          brief,
          memberHref: null,
          overloadIndex: 0,
        });
      }
    }
    if (members.length > 0) {
      sections.push({
        title: pageType === 'module' ? 'Packages' : 'Types',
        members,
      });
    }
  }

  return {
    pageType,
    title,
    declaration,
    description,
    sections: splitSections(sections),
  };
}

function parseTableRows(tableEl, dokkaBaseUrl) {
  const parts = [];
  const rows = tableEl.querySelectorAll('.table-row');
  for (const row of rows) {
    const nameEl = row.querySelector(
      'u, .main-subrow > div:first-child',
    );
    const descEl = row.querySelector(
      '.title, .main-subrow > div:last-child .paragraph',
    );
    const pName = nameEl
      ? nameEl.textContent.replace(/\s+/g, '').trim()
      : '';
    const pDesc = descEl
      ? cleanHtml(descEl.innerHTML, dokkaBaseUrl)
      : '';
    if (!pName) continue;
    const hasLink = nameEl && nameEl.querySelector('a');
    if (hasLink) {
      const nameHtml = cleanHtml(nameEl.innerHTML, dokkaBaseUrl);
      parts.push(
        `<div class="${styles.paramRow}">${nameHtml}${pDesc ? ' &ndash; ' + pDesc : ''}</div>`,
      );
    } else {
      parts.push(
        `<div class="${styles.paramRow}"><code class="${styles.paramName}">${escapeHtml(pName)}</code>${pDesc ? ' &ndash; ' + pDesc : ''}</div>`,
      );
    }
  }
  return parts.join('');
}

function parseDocContent(container, dokkaBaseUrl) {
  const parts = [];
  for (const el of container.children) {
    if (el.nodeType !== 1) continue;
    if (
      el.classList.contains('symbol') &&
      el.classList.contains('monospace')
    )
      continue;
    if (el.classList.contains('sample-container')) {
      const code = el.querySelector('code');
      if (code) {
        const text = code.textContent.replace(/^\n+/, '');
        parts.push(
          `<pre class="${styles.codeBlock}"><code>${colorizeCodeBlock(escapeHtml(text))}</code></pre>`,
        );
      }
    } else if (el.tagName === 'H4' || el.tagName === 'H3') {
      parts.push(
        `<h4 class="${styles.descHeader}">${escapeHtml(el.textContent)}</h4>`,
      );
    } else if (el.classList.contains('kdoc-tag')) {
      const h = el.querySelector('h4');
      if (h) {
        parts.push(
          `<h4 class="${styles.descHeader}">${escapeHtml(h.textContent)}</h4>`,
        );
      }
      const paras = el.querySelectorAll('.paragraph, p');
      for (const p of paras) {
        parts.push(`<p>${cleanHtml(p.innerHTML, dokkaBaseUrl)}</p>`);
      }
    } else if (
      el.classList.contains('table') &&
      el.querySelector('.table-row')
    ) {
      parts.push(parseTableRows(el, dokkaBaseUrl));
    } else if (
      el.classList.contains('paragraph') ||
      el.tagName === 'UL' ||
      el.tagName === 'OL' ||
      el.tagName === 'P'
    ) {
      parts.push(cleanHtml(el.innerHTML, dokkaBaseUrl));
    }
  }
  return parts.join('');
}

function parseMemberPageSections(htmlString, dokkaBaseUrl) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlString, 'text/html');
  const content =
    doc.querySelector('#content') || doc.querySelector('.main-content');
  if (!content) return [];

  const hinted = content.querySelector('.platform-hinted .content');
  if (!hinted) return [];

  const sections = [];
  let currentParts = [];

  for (const el of hinted.children) {
    if (el.nodeType !== 1) continue;
    if (el.tagName === 'HR') {
      sections.push(currentParts.join(''));
      currentParts = [];
      continue;
    }
    if (
      el.classList.contains('symbol') &&
      el.classList.contains('monospace')
    )
      continue;

    if (el.classList.contains('sample-container')) {
      const code = el.querySelector('code');
      if (code) {
        const text = code.textContent.replace(/^\n+/, '');
        currentParts.push(
          `<pre class="${styles.codeBlock}"><code>${colorizeCodeBlock(escapeHtml(text))}</code></pre>`,
        );
      }
    } else if (el.tagName === 'H4' || el.tagName === 'H3') {
      currentParts.push(
        `<h4 class="${styles.descHeader}">${escapeHtml(el.textContent)}</h4>`,
      );
    } else if (el.classList.contains('kdoc-tag')) {
      const h = el.querySelector('h4');
      if (h) {
        currentParts.push(
          `<h4 class="${styles.descHeader}">${escapeHtml(h.textContent)}</h4>`,
        );
      }
      const paras = el.querySelectorAll('.paragraph, p');
      for (const p of paras) {
        currentParts.push(`<p>${cleanHtml(p.innerHTML, dokkaBaseUrl)}</p>`);
      }
    } else if (
      el.classList.contains('table') &&
      el.querySelector('.table-row')
    ) {
      currentParts.push(parseTableRows(el, dokkaBaseUrl));
    } else if (
      el.classList.contains('paragraph') ||
      el.tagName === 'UL' ||
      el.tagName === 'OL' ||
      el.tagName === 'P'
    ) {
      currentParts.push(cleanHtml(el.innerHTML, dokkaBaseUrl));
    }
  }

  if (currentParts.length > 0) {
    sections.push(currentParts.join(''));
  }

  return sections;
}

function splitSections(sections) {
  const result = [];
  for (const section of sections) {
    if (section.title === 'Properties') {
      const statics = section.members.filter((m) => m.isStatic);
      const instance = section.members.filter((m) => !m.isStatic);
      if (statics.length > 0)
        result.push({title: 'Static Properties', members: statics});
      if (instance.length > 0)
        result.push({title: 'Properties', members: instance});
    } else if (section.title === 'Functions') {
      const statics = section.members.filter((m) => m.isStatic);
      const instance = section.members.filter((m) => !m.isStatic);
      if (statics.length > 0)
        result.push({title: 'Static Methods', members: statics});
      if (instance.length > 0)
        result.push({title: 'Methods', members: instance});
    } else {
      result.push(section);
    }
  }
  return result;
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function cleanHtml(html, dokkaBaseUrl) {
  let cleaned = html
    .replace(/<wbr\s*\/?>/gi, '')
    .replace(
      /<span class="anchor-wrapper">[\s\S]*?<\/span>\s*<\/span>/gi,
      '',
    )
    .replace(/\s*data-[\w-]+="[^"]*"/gi, '');

  cleaned = cleaned.replace(
    /<a\s+href="[^"]*kotlin[^"]*"[^>]*>(\w+)<\/a>/gi,
    (_, type) => `<code>${KOTLIN_JAVA_TYPES[type] || type}</code>`,
  );
  cleaned = cleaned.replace(
    /<a\s+href="[^"]*oracle\.com[^"]*"[^>]*>(\w+)<\/a>/gi,
    (_, type) => `<code>${type}</code>`,
  );

  cleaned = cleaned.replace(
    /<a\s+href="([^"]+)"([^>]*)>([\s\S]*?)<\/a>/gi,
    (full, href, rest, linkText) => {
      if (href.startsWith('http://') || href.startsWith('https://'))
        return full;
      const text = linkText.replace(/<[^>]+>/g, '').trim();
      return `<span class="api-internal-link" data-link-text="${text.replace(/"/g, '&quot;')}">${linkText}</span>`;
    },
  );

  return cleaned;
}

function commonPrefix(packages) {
  if (!packages.length) return '';
  const sorted = [...packages].sort();
  const first = sorted[0].split('.');
  const last = sorted[sorted.length - 1].split('.');
  let i = 0;
  while (i < first.length && i < last.length && first[i] === last[i]) i++;
  return first.slice(0, i).join('.');
}

function buildPackageTree(packagesObj) {
  if (
    !packagesObj ||
    typeof packagesObj !== 'object' ||
    Array.isArray(packagesObj)
  )
    return {nodes: [], rootPkg: null};

  const pkgNames = Object.keys(packagesObj).sort();
  if (!pkgNames.length) return {nodes: [], rootPkg: null};

  const prefix = commonPrefix(pkgNames);
  const root = {};
  let rootPkg = null;

  for (const pkg of pkgNames) {
    let relative = pkg;
    if (prefix && pkg.startsWith(prefix)) {
      relative = pkg.slice(prefix.length);
      if (relative.startsWith('.')) relative = relative.slice(1);
    }

    const classes = Array.isArray(packagesObj[pkg]) ? packagesObj[pkg] : [];

    if (!relative) {
      rootPkg = {
        fullPackage: pkg,
        classes,
        display: prefix.split('.').pop() || prefix,
      };
      continue;
    }

    const parts = relative.split('.');
    let node = root;
    for (let i = 0; i < parts.length; i++) {
      const seg = parts[i];
      if (!node[seg]) {
        node[seg] = {_children: {}, _classes: [], _fullPkg: null, _seg: seg};
      }
      if (i === parts.length - 1) {
        node[seg]._fullPkg = pkg;
        node[seg]._classes = classes;
      }
      node = node[seg]._children;
    }
  }

  function toNodeArray(obj) {
    return Object.keys(obj)
      .sort()
      .map((key) => {
        const entry = obj[key];
        return {
          segment: entry._seg,
          fullPackage: entry._fullPkg,
          classes: entry._classes || [],
          children: toNodeArray(entry._children || {}),
        };
      });
  }

  return {nodes: toNodeArray(root), rootPkg};
}

function dokkaClassSegment(className) {
  return (
    '-' +
    className.replace(/([A-Z])/g, (m, c, offset) =>
      (offset > 0 ? '-' : '') + c.toLowerCase(),
    )
  );
}

function sourceUrl(moduleId, pkg, className) {
  const pkgPath = pkg.replace(/\./g, '/');
  return `${GITHUB_TREE}/${moduleId}/src/main/java/${pkgPath}/${className}.java`;
}

function TreeNode({node, expanded, setExpanded, onSelect}) {
  if (!node) return null;
  const hasChildren = node.children && node.children.length > 0;
  const hasClasses = node.classes && node.classes.length > 0;
  const key = node.fullPackage || node.segment;
  const isOpen = expanded.has(key);

  if (!hasChildren && !hasClasses) return null;

  const toggle = () => {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  };

  return (
    <li className={styles.treeItem}>
      <button type="button" className={styles.folderRow} onClick={toggle}>
        <span className={isOpen ? styles.folderOpen : styles.folderClosed} />
        <span className={styles.folderName}>{node.segment}</span>
      </button>
      {isOpen && (
        <div className={styles.treeNest}>
          {hasChildren &&
            node.children.map((child) => (
              <TreeNode
                key={child.segment}
                node={child}
                expanded={expanded}
                setExpanded={setExpanded}
                onSelect={onSelect}
              />
            ))}
          {node.fullPackage && (
            <button
              type="button"
              className={`${styles.classLink} ${styles.overviewLink}`}
              onClick={() =>
                onSelect({type: 'package', pkg: node.fullPackage})
              }>
              (package overview)
            </button>
          )}
          {hasClasses &&
            node.fullPackage &&
            node.classes.map((cn) => (
              <button
                key={cn}
                type="button"
                className={styles.classLink}
                onClick={() =>
                  onSelect({
                    type: 'class',
                    pkg: node.fullPackage,
                    className: cn,
                  })
                }>
                {cn}
              </button>
            ))}
        </div>
      )}
    </li>
  );
}

function PackageSidebar({nodes, rootPkg, expanded, setExpanded, onSelect}) {
  if (!nodes || (!nodes.length && !rootPkg)) {
    return (
      <p className={styles.treeHint}>No packages found for this module.</p>
    );
  }

  return (
    <>
      <p className={styles.treeHint}>Expand a folder to see its classes.</p>
      <ul className={styles.treeList}>
        {nodes.map((node) => (
          <TreeNode
            key={node.segment}
            node={node}
            expanded={expanded}
            setExpanded={setExpanded}
            onSelect={onSelect}
          />
        ))}
      </ul>
      {rootPkg && (
        <div className={styles.rootSection}>
          <button
            type="button"
            className={`${styles.classLink} ${styles.overviewLink}`}
            onClick={() =>
              onSelect({type: 'package', pkg: rootPkg.fullPackage})
            }>
            (package overview)
          </button>
          {rootPkg.classes.map((cn) => (
            <button
              key={cn}
              type="button"
              className={styles.classLink}
              onClick={() =>
                onSelect({
                  type: 'class',
                  pkg: rootPkg.fullPackage,
                  className: cn,
                })
              }>
              {cn}
            </button>
          ))}
        </div>
      )}
    </>
  );
}

function DocContent({doc, selection, moduleId, onLinkClick, memberDocs}) {
  if (!doc) return null;

  const isClass = selection?.type === 'class';
  const pkg = selection?.pkg || '';
  const className = selection?.className || '';

  let typeLabel = '';
  if (doc.pageType === 'classlike') {
    if (/\binterface\b/.test(doc.declaration)) typeLabel = 'INTERFACE';
    else if (/\benum\b/.test(doc.declaration)) typeLabel = 'ENUM';
    else typeLabel = 'CLASS';
  } else if (doc.pageType === 'package') {
    typeLabel = 'PACKAGE';
  }

  const handleContentClick = useCallback(
    (e) => {
      const link = e.target.closest('.api-internal-link');
      if (!link) return;
      e.preventDefault();
      const text =
        link.getAttribute('data-link-text') || link.textContent.trim();
      if (onLinkClick) onLinkClick(text);
    },
    [onLinkClick],
  );

  return (
    <div className={styles.docRoot} onClick={handleContentClick}>
      <header className={styles.docHeader}>
        <div>
          {typeLabel && (
            <span className={styles.docPageType}>{typeLabel}</span>
          )}
          <h1 className={styles.docTitle}>
            {isClass ? className : doc.title}
          </h1>
          {pkg && <p className={styles.docPackage}>{pkg}</p>}
        </div>
        {isClass && (
          <a
            className={styles.viewSourceBtn}
            href={sourceUrl(moduleId, pkg, className)}
            target="_blank"
            rel="noopener noreferrer">
            <svg
              className={styles.viewSourceIcon}
              viewBox="0 0 16 16"
              fill="currentColor"
              aria-hidden>
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" />
            </svg>
            View source
          </a>
        )}
      </header>

      {doc.declaration && (
        <pre className={styles.declarationBlock}>
          <code>{colorizeSignature(doc.declaration)}</code>
        </pre>
      )}

      {doc.description && (
        <div
          className={styles.docDescription}
          dangerouslySetInnerHTML={{__html: doc.description}}
        />
      )}

      {doc.sections.map((section) => (
        <section key={section.title} className={styles.memberSection}>
          <h2 className={styles.memberSectionTitle}>{section.title}</h2>
          {section.members.map((member, i) => (
            <MemberCard
              key={`${member.name}-${i}`}
              member={member}
              memberDocs={memberDocs}
            />
          ))}
        </section>
      ))}
    </div>
  );
}

function MemberCard({member, memberDocs}) {
  const fullDoc =
    memberDocs?.[member.memberHref]?.[member.overloadIndex] || null;

  return (
    <div id={`member-${member.name}`} className={styles.memberCard}>
      {member.signature && (
        <pre className={styles.memberSignature}>
          <code>{colorizeSignature(member.signature)}</code>
        </pre>
      )}
      {!member.signature && member.name && (
        <div className={styles.memberNameOnly}>{member.name}</div>
      )}
      {fullDoc ? (
        <div
          className={styles.memberFullDoc}
          dangerouslySetInnerHTML={{__html: fullDoc}}
        />
      ) : (
        member.brief && (
          <div
            className={styles.memberBrief}
            dangerouslySetInnerHTML={{__html: member.brief}}
          />
        )
      )}
    </div>
  );
}

export default function ApiPage() {
  const indexUrl = useBaseUrl('/api-class-index.json');
  const baseUrl = useBaseUrl(DOKKA_PREFIX);

  const [moduleId, setModuleId] = useState('flixelgdx-core');
  const [index, setIndex] = useState(null);
  const [indexError, setIndexError] = useState(null);
  const [expanded, setExpanded] = useState(() => new Set());
  const [selection, setSelection] = useState(null);
  const [doc, setDoc] = useState(null);
  const [loading, setLoading] = useState(false);
  const [contentError, setContentError] = useState(null);
  const [dokkaBase, setDokkaBase] = useState('');
  const [memberDocs, setMemberDocs] = useState({});
  const contentRef = useRef(null);
  const fetchIdRef = useRef(0);

  useEffect(() => {
    let cancelled = false;
    setIndexError(null);
    fetch(indexUrl)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data) => {
        if (!cancelled) setIndex(data);
      })
      .catch(() => {
        if (!cancelled) {
          setIndex(null);
          setIndexError(
            'Could not load api-class-index.json. Run: ./gradlew writeApiClassIndex',
          );
        }
      });
    return () => {
      cancelled = true;
    };
  }, [indexUrl]);

  const moduleOptions = useMemo(() => {
    if (!index || typeof index !== 'object' || Array.isArray(index))
      return MODULE_ORDER;
    const available = new Set(
      Object.keys(index).filter(
        (id) =>
          index[id] &&
          typeof index[id] === 'object' &&
          !Array.isArray(index[id]),
      ),
    );
    const ordered = MODULE_ORDER.filter((m) => available.has(m.id));
    for (const id of available) {
      if (!ordered.some((m) => m.id === id)) {
        ordered.push({
          id,
          label: MODULE_LABELS[id] ?? id.replace(/^flixelgdx-/, ''),
        });
      }
    }
    return ordered.length ? ordered : MODULE_ORDER;
  }, [index]);

  useEffect(() => {
    if (!moduleOptions.some((m) => m.id === moduleId)) {
      setModuleId(moduleOptions[0].id);
    }
  }, [moduleId, moduleOptions]);

  const packagesForModule = useMemo(() => {
    const raw = index?.[moduleId];
    return raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {};
  }, [index, moduleId]);

  const {nodes, rootPkg} = useMemo(
    () => buildPackageTree(packagesForModule),
    [packagesForModule],
  );

  useEffect(() => {
    setSelection(null);
    setDoc(null);
    setMemberDocs({});
    if (nodes && nodes.length > 0) {
      const firstKey = nodes[0].fullPackage || nodes[0].segment;
      setExpanded(new Set([firstKey]));
    } else {
      setExpanded(new Set());
    }
  }, [moduleId, nodes]);

  const fetchDocPage = useCallback(
    (sel) => {
      setSelection(sel);
      setContentError(null);
      setLoading(true);
      setMemberDocs({});

      const id = ++fetchIdRef.current;
      let dokkaPath;
      if (sel.type === 'package') {
        dokkaPath = `${baseUrl}/${moduleId}/${moduleId}/${sel.pkg}/index.html`;
      } else {
        dokkaPath = `${baseUrl}/${moduleId}/${moduleId}/${sel.pkg}/${dokkaClassSegment(sel.className)}/index.html`;
      }

      fetch(dokkaPath)
        .then((r) => {
          if (!r.ok) throw new Error(`HTTP ${r.status}`);
          return r.text();
        })
        .then((html) => {
          if (id !== fetchIdRef.current) return;
          const classDir = dokkaPath.replace(/\/[^/]*$/, '/');
          setDokkaBase(classDir);
          const fullBase = `${window.location.origin}${classDir}`;
          const parsed = parseDokkaHtml(html, sel.className, fullBase);
          if (!parsed) {
            setContentError('Could not parse documentation page.');
            setDoc(null);
          } else {
            setDoc(parsed);
            fetchMemberDocs(parsed, classDir, id);
          }
          setLoading(false);
        })
        .catch((err) => {
          if (id !== fetchIdRef.current) return;
          setContentError(`Failed to load: ${err.message}`);
          setDoc(null);
          setLoading(false);
        });
    },
    [baseUrl, moduleId],
  );

  const fetchMemberDocs = useCallback(
    (parsedDoc, classDir, fetchId) => {
      const hrefs = new Set();
      for (const section of parsedDoc.sections) {
        for (const member of section.members) {
          if (member.memberHref) hrefs.add(member.memberHref);
        }
      }
      if (hrefs.size === 0) return;

      const docsMap = {};
      const promises = [...hrefs].map((href) =>
        fetch(`${classDir}${href}`)
          .then((r) => (r.ok ? r.text() : ''))
          .then((html) => {
            if (html && fetchId === fetchIdRef.current) {
              docsMap[href] = parseMemberPageSections(html, classDir);
            }
          })
          .catch(() => {}),
      );

      Promise.all(promises).then(() => {
        if (fetchId === fetchIdRef.current) {
          setMemberDocs({...docsMap});
        }
      });
    },
    [],
  );

  const handleLinkClick = useCallback(
    (linkText) => {
      if (!index) return;

      const clean = linkText.replace(/\(\)$/, '').trim();

      const memberEl = contentRef.current?.querySelector(
        `[id="member-${clean}"]`,
      );
      if (memberEl) {
        memberEl.scrollIntoView({behavior: 'smooth', block: 'center'});
        memberEl.classList.add(styles.memberHighlight);
        setTimeout(
          () => memberEl.classList.remove(styles.memberHighlight),
          1500,
        );
        return;
      }

      const dotIdx = clean.indexOf('.');
      if (dotIdx !== -1) {
        const cls = clean.slice(0, dotIdx);
        const member = clean.slice(dotIdx + 1);
        const findAndNavigate = (modId, pkgs) => {
          for (const [pkg, classes] of Object.entries(pkgs)) {
            if (Array.isArray(classes) && classes.includes(cls)) {
              if (modId !== moduleId) setModuleId(modId);
              const navigate = () => {
                fetchDocPage({type: 'class', pkg, className: cls});
                if (member) {
                  setTimeout(() => {
                    const el = contentRef.current?.querySelector(
                      `[id="member-${member}"]`,
                    );
                    if (el) {
                      el.scrollIntoView({behavior: 'smooth', block: 'center'});
                      el.classList.add(styles.memberHighlight);
                      setTimeout(
                        () => el.classList.remove(styles.memberHighlight),
                        1500,
                      );
                    }
                  }, 500);
                }
              };
              if (modId !== moduleId) {
                setTimeout(navigate, 100);
              } else {
                navigate();
              }
              return true;
            }
          }
          return false;
        };
        if (index[moduleId] && findAndNavigate(moduleId, index[moduleId]))
          return;
        for (const [modId, modPkgs] of Object.entries(index)) {
          if (modId === moduleId) continue;
          if (findAndNavigate(modId, modPkgs)) return;
        }
      }

      const pkgs = index[moduleId];
      if (pkgs) {
        for (const [pkg, classes] of Object.entries(pkgs)) {
          if (Array.isArray(classes) && classes.includes(clean)) {
            fetchDocPage({type: 'class', pkg, className: clean});
            return;
          }
        }
      }
      for (const [modId, modPkgs] of Object.entries(index)) {
        if (modId === moduleId) continue;
        for (const [pkg, classes] of Object.entries(modPkgs)) {
          if (Array.isArray(classes) && classes.includes(clean)) {
            setModuleId(modId);
            setTimeout(() => {
              fetchDocPage({type: 'class', pkg, className: clean});
            }, 100);
            return;
          }
        }
      }
    },
    [index, moduleId, fetchDocPage],
  );

  useEffect(() => {
    if (contentRef.current) contentRef.current.scrollTop = 0;
  }, [doc]);

  return (
    <Layout description="FlixelGDX API reference">
      <Head>
        <title>FlixelGDX | API Reference</title>
      </Head>
      <main className={styles.wrap}>
        <div className={styles.toolbar}>
          <label className={styles.moduleLabel} htmlFor="api-module">
            Module
          </label>
          <select
            id="api-module"
            className={styles.moduleSelect}
            value={moduleId}
            onChange={(e) => setModuleId(e.target.value)}>
            {moduleOptions.map((m) => (
              <option key={m.id} value={m.id}>
                {m.label}
              </option>
            ))}
          </select>
        </div>

        {indexError && <p className={styles.contentError}>{indexError}</p>}

        <div className={styles.layout}>
          <aside className={styles.sidebar} aria-label="API packages">
            <PackageSidebar
              nodes={nodes}
              rootPkg={rootPkg}
              expanded={expanded}
              setExpanded={setExpanded}
              onSelect={fetchDocPage}
            />
          </aside>

          <div className={styles.contentArea} ref={contentRef}>
            {loading && (
              <p className={styles.contentLoading}>
                Loading documentation...
              </p>
            )}
            {contentError && (
              <p className={styles.contentError}>{contentError}</p>
            )}
            {!loading && !contentError && !doc && (
              <div className={styles.contentPlaceholder}>
                <p>
                  Select a package or class from the sidebar to view its
                  documentation.
                </p>
              </div>
            )}
            {!loading && doc && (
              <DocContent
                doc={doc}
                selection={selection}
                moduleId={moduleId}
                onLinkClick={handleLinkClick}
                memberDocs={memberDocs}
              />
            )}
          </div>
        </div>
      </main>
    </Layout>
  );
}
