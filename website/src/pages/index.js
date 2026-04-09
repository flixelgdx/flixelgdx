import Head from '@docusaurus/Head';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import clsx from 'clsx';
import {useEffect, useRef} from 'react';

import styles from './index.module.css';

const HX_LOGO = 'https://avatars.githubusercontent.com/u/4309553?s=400&v=4';
const LIBGDX_LOGO = 'https://libgdx.com/assets/images/logo.png';
const GITHUB_MARK_PATH = '/img/github-mark.svg';

function HookPanel({children, imgSrc, sectionClass, layout, largeLogo, githubLogo}) {
  const sectionRef = useRef(null);
  const imgRef = useRef(null);
  const artFirst = layout === 'textRight';

  useEffect(() => {
    const img = imgRef.current;
    const section = sectionRef.current;
    if (!img || !section) return undefined;

    const onScroll = () => {
      const rect = section.getBoundingClientRect();
      const vh = window.innerHeight || 1;
      const center = rect.top + rect.height / 2;
      const n = (center - vh * 0.5) / vh;
      const offset = Math.max(-40, Math.min(40, n * 32));
      const baseTransform = `translate3d(0, ${offset}px, 0)`;
      if (img.classList.contains(styles.hookLogoVisible)) {
        img.style.transform = baseTransform;
      }
    };

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          img.classList.add(styles.hookLogoVisible);
        }
      },
      {threshold: 0.15},
    );
    observer.observe(section);

    window.addEventListener('scroll', onScroll, {passive: true});
    return () => {
      window.removeEventListener('scroll', onScroll);
      observer.disconnect();
    };
  }, []);

  const textCol = (
    <div className={styles.hookTextWrap}>
      <div className={styles.hookTextInner}>{children}</div>
    </div>
  );

  const artCol = (
    <div className={styles.hookArtWrap} aria-hidden>
      <img
        ref={imgRef}
        className={clsx(
          styles.hookLogo,
          styles.hookLogoParallax,
          largeLogo && styles.hookLogoLarge,
          githubLogo && styles.hookLogoGithub,
          artFirst && styles.hookLogoFromLeft,
        )}
        src={imgSrc}
        alt=""
        loading="lazy"
      />
    </div>
  );

  return (
    <section
      ref={sectionRef}
      className={clsx(
        styles.hookPanel,
        artFirst ? styles.hookPanelArtFirst : styles.hookPanelArtSecond,
        sectionClass,
      )}>
      {artFirst ? (
        <>
          {artCol}
          {textCol}
        </>
      ) : (
        <>
          {textCol}
          {artCol}
        </>
      )}
    </section>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  const logoUrl = useBaseUrl('/img/flixelgdx-logo.png');
  const githubMarkUrl = useBaseUrl(GITHUB_MARK_PATH);

  return (
    <Layout description={siteConfig.tagline}>
      <Head>
        <title>FlixelGDX | Home</title>
      </Head>
      <main className={styles.pageRoot}>
        <header className={clsx('hero', styles.heroHome)}>
          <div className={styles.heroInner}>
            <img
              className={styles.heroLogoMain}
              src={logoUrl}
              alt="FlixelGDX"
            />
            <p className={styles.heroTagline}>
              The most powerful game development framework for Java, made for
              beginners and experts alike.
            </p>
            <div className={styles.heroActions}>
              <Link className="button button--secondary button--lg" to="/getting-started">
                Get started
              </Link>
              <Link className="button button--outline button--secondary button--lg" to="/api">
                API reference
              </Link>
            </div>
          </div>
        </header>

        <HookPanel sectionClass={styles.sectionPink} layout="textLeft" imgSrc={HX_LOGO}>
          <p>
            Directly inspired by its Haxe-based cousin, FlixelGDX brings the
            simple syntax and &quot;just works&quot; philosophy to Java,
            completely written from the ground up just for you.
          </p>
        </HookPanel>

        <HookPanel
          sectionClass={styles.sectionYellow}
          layout="textRight"
          largeLogo
          imgSrc={LIBGDX_LOGO}>
          <p>
            Built on top of another powerful framework, it seamlessly integrates
            into the libGDX ecosystem, allowing you to use other libraries
            alongside FlixelGDX with zero issues.
          </p>
        </HookPanel>

        <HookPanel
          sectionClass={styles.sectionCyan}
          layout="textLeft"
          githubLogo
          imgSrc={githubMarkUrl}>
          <p>
            Completely open sourced on GitHub, anyone can contribute and help
            FlixelGDX grow to become even better.
          </p>
        </HookPanel>

        <p className={styles.demosFooter}>Demos coming soon!</p>
      </main>
    </Layout>
  );
}
