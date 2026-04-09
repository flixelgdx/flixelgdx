import Head from '@docusaurus/Head';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import {ProjectGeneratorForm} from '../components/ProjectGenerator/ProjectGeneratorForm';

export default function GettingStartedPage() {
  return (
    <Layout description="Generate a FlixelGDX Gradle project for your platforms">
      <Head>
        <title>FlixelGDX | Getting Started</title>
      </Head>
      <div className="container margin-vert--lg">
        <Heading as="h1">Getting Started</Heading>
        <ProjectGeneratorForm />
      </div>
    </Layout>
  );
}
