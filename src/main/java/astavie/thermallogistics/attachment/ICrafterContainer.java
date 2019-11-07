package astavie.thermallogistics.attachment;

public interface ICrafterContainer extends IRequesterContainer {

	ICrafter<?> getCrafter(int index);

	@Override
	default IRequester<?> getRequester(int index) {
		return getCrafter(index);
	}

}
