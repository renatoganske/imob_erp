import { EditPropertyClient } from "./EditPropertyClient";

export default async function EditPropertyPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <EditPropertyClient id={id} />;
}
